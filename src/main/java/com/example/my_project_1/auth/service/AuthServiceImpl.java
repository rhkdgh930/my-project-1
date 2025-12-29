package com.example.my_project_1.auth.service;

import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.auth.filter.JwtAuthenticationException;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;
    private final UserDetailsService userDetailsService;

    public TokenResponse reissue(String refreshToken) {

        if (jwtProvider.isExpired(refreshToken)) {
            throw new JwtAuthenticationException(ErrorCode.EXPIRED_TOKEN);
        }

        if (!jwtProvider.isRefreshToken(refreshToken)) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN_TYPE);
        }

        String email = jwtProvider.getEmail(refreshToken);
        String savedToken = redisTokenService.getRefreshToken(email);

        if (savedToken == null || !refreshToken.equals(savedToken)) {
            redisTokenService.deleteRefreshToken(email);
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(email);

        // 새 토큰 발급
        String newAccessToken =
                jwtProvider.createAccessToken(
                        userDetails.getUserId(),
                        userDetails.getUsername(),
                        userDetails.getRole()
                );

        String newRefreshToken =
                jwtProvider.createRefreshToken(email);

        redisTokenService.saveRefreshToken(
                email,
                newRefreshToken,
                jwtProvider.getRemainingValidityMillis(newRefreshToken)
        );

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String accessToken) {

        if (jwtProvider.isExpired(accessToken)) {
            throw new JwtAuthenticationException(ErrorCode.EXPIRED_TOKEN);
        }

        String email = jwtProvider.getEmail(accessToken);

        redisTokenService.deleteRefreshToken(email);

        redisTokenService.blacklistAccessToken(
                accessToken,
                jwtProvider.getRemainingValidityMillis(accessToken)
        );
    }
}
