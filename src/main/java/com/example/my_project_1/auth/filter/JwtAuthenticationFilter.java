package com.example.my_project_1.auth.filter;

import com.example.my_project_1.auth.cache.CachedUserContext;
import com.example.my_project_1.auth.constant.SecurityConstants;
import com.example.my_project_1.auth.exception.JwtAuthenticationException;
import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;
    private final RedisUserContextService redisUserContextService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtProvider.parseClaimsSafely(token);
            jwtProvider.assertAccessToken(claims);

            if (redisTokenService.isBlacklisted(token)) {
                throw new JwtAuthenticationException(ErrorCode.LOGOUT_USER);
            }

            Long userId = Long.parseLong(claims.getSubject());
            CachedUserContext ctx = redisUserContextService.getUserContext(userId);
            redisUserContextService.validateActiveUser(ctx);

            setAuthentication(ctx);
            filterChain.doFilter(request, response);

        } catch (JwtAuthenticationException e) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, e);
        } catch (CustomException e) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, new JwtAuthenticationException(e.getErrorCode()));
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            log.error(
                    "[SECURITY][JwtAuthenticationFilter][AUTHENTICATION_ERROR] uri={} errorType={}",
                    request.getRequestURI(),
                    e.getClass().getSimpleName(),
                    e
            );
            authenticationEntryPoint.commence(request, response, new JwtAuthenticationException(ErrorCode.AUTHENTICATION_FAILED));
        }
    }

    private void setAuthentication(CachedUserContext ctx) {
        UserDetailsImpl userDetails = UserDetailsImpl.from(ctx);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(SecurityConstants.AUTHORIZATION);
        return (StringUtils.hasText(bearer) && bearer.startsWith(SecurityConstants.BEARER))
                ? bearer.substring(7)
                : null;
    }
}
