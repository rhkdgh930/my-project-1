package com.example.my_project_1.auth.service;

import com.example.my_project_1.auth.cache.CachedUserContext;
import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;
    private final RedisUserContextService userContextService;

    @Transactional
    public TokenResponse reissue(String refreshToken) {
        String requestHash = redisTokenService.getHash(refreshToken);

        TokenResponse cachedResponse = redisTokenService.getReissueHistory(requestHash);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        Claims claims = jwtProvider.parseClaimsSafely(refreshToken);
        jwtProvider.assertRefreshToken(claims);

        Long userId = Long.valueOf(claims.getSubject());

        String savedRTHash = redisTokenService.getRefreshTokenHash(userId);

        if (savedRTHash == null || !savedRTHash.equals(requestHash)) {
            redisTokenService.deleteRefreshTokenHash(userId);
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        CachedUserContext ctx = userContextService.getUserContext(userId);

        String newAccessToken =
                jwtProvider.createAccessToken(
                        userId,
                        ctx.getRole().name()
                );

        String newRefreshToken =
                jwtProvider.createRefreshToken(userId);

        redisTokenService.saveRefreshTokenHash(
                userId,
                newRefreshToken,
                jwtProvider.getRemainingValidityMillis(newRefreshToken)
        );

        TokenResponse response = new TokenResponse(newAccessToken, newRefreshToken);
        redisTokenService.saveReissueHistory(requestHash, response);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String accessToken) {

        Claims claims = jwtProvider.parseClaimsSafely(accessToken);
        assertNotExpired(claims);

        Long userId = Long.valueOf(claims.getSubject());

        redisTokenService.deleteRefreshTokenHash(userId);

        redisTokenService.blacklistAccessToken(
                accessToken,
                jwtProvider.getRemainingValidityMillis(accessToken)
        );
    }

    private void assertNotExpired(Claims claims) {
        Date exp = claims.getExpiration();
        if (exp.before(new Date())) {
            throw new CustomException(ErrorCode.EXPIRED_ACCESS_TOKEN);
        }
    }
}
