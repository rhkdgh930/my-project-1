package com.example.my_project_1.auth.filter.authz;

import com.example.my_project_1.auth.constant.SecurityConstants;
import com.example.my_project_1.auth.filter.JwtAuthenticationException;
import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            if (!jwtProvider.isValid(token)) {
                throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
            }
            if (jwtProvider.isExpired(token)) {
                throw new JwtAuthenticationException(ErrorCode.EXPIRED_TOKEN);
            }
            if (!jwtProvider.isAccessToken(token)) {
                throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
            }
            if (redisTokenService.isBlacklisted(token)) {
                throw new JwtAuthenticationException(ErrorCode.REVOKED_TOKEN);
            }

            setAuthentication(token);
        }
        filterChain.doFilter(request, response);
    }

    private void setAuthentication(String token) {
        Long userId = jwtProvider.getUserId(token);
        String email = jwtProvider.getEmail(token);
        String role = jwtProvider.getRole(token);

        UserDetails userDetails = new UserDetailsImpl(userId, email, null, role);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(SecurityConstants.TOKEN_HEADER);
        return (StringUtils.hasText(bearer) && bearer.startsWith(SecurityConstants.TOKEN_PREFIX))
                ? bearer.substring(7)
                : null;
    }
}