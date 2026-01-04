package com.example.my_project_1.auth.service;

import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.auth.service.userdetails.UserDetailsImpl;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;
    private final UserDetailsService userDetailsService;

    public TokenResponse reissue(String refreshToken) {

        Claims claims = jwtProvider.parseClaimsSafely(refreshToken);
        jwtProvider.validateRefreshToken(claims);

        String email = claims.getSubject();
        String savedToken = redisTokenService.getRefreshToken(email);

        if (savedToken == null || !refreshToken.equals(savedToken)) {
            redisTokenService.deleteRefreshToken(email);
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
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

        Claims claims = jwtProvider.parseClaimsSafely(accessToken);
        assertNotExpired(claims);

        String email = claims.getSubject();

        redisTokenService.deleteRefreshToken(email);

        redisTokenService.blacklistAccessToken(
                accessToken,
                jwtProvider.getRemainingValidityMillis(accessToken)
        );
    }

    private static void assertNotExpired(Claims claims) {
        Date exp = claims.getExpiration();
        if (exp.before(new Date())) {
            throw new CustomException(ErrorCode.EXPIRED_ACCESS_TOKEN);
        }
    }
}

