package com.example.my_project_1.auth.filter;

import com.example.my_project_1.auth.cache.CachedUserContext;
import com.example.my_project_1.auth.constant.SecurityConstants;
import com.example.my_project_1.auth.exception.JwtAuthenticationException;
import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.auth.service.userdetails.UserDetailsImpl;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.AccountStatus;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;
    private final RedisUserContextService userContextService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtProvider.parseClaimsSafely(token);
        jwtProvider.validateAccessToken(claims);

        if (redisTokenService.isBlacklisted(token)) {
            throw new JwtAuthenticationException(ErrorCode.LOGOUT_USER);
        }

        Long userId = claims.get("uid", Long.class);
        CachedUserContext ctx = userContextService.getUserContext(userId);

        validateUser(ctx);
        setAuthentication(ctx);

        filterChain.doFilter(request, response);
    }

    private void validateUser(CachedUserContext ctx) {
        if (ctx.isDeleted()) {
            throw new JwtAuthenticationException(ErrorCode.USER_NOT_FOUND);
        }

        if (ctx.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new JwtAuthenticationException(ErrorCode.USER_SUSPENDED);
        }
    }

    private void setAuthentication(CachedUserContext ctx) {
        UserDetailsImpl userDetails =
                new UserDetailsImpl(
                        ctx.getUserId(),
                        ctx.getEmail(),
                        null,
                        ctx.getRole().name()
                );

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
