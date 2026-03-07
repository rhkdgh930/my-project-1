package com.example.my_project_1.auth.service.impl;

import com.example.my_project_1.auth.cache.CachedUserContext;
import com.example.my_project_1.auth.exception.JwtAuthenticationException;
import com.example.my_project_1.auth.service.AuthService;
import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.auth.service.request.LoginRequest;
import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.event.UserAccountChangedEvent;
import com.example.my_project_1.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final Clock clock;

    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTokenService redisTokenService;
    private final RedisUserContextService userContextService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
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
    @Override
    public TokenResponse restoreAccount(LoginRequest request) {
        User user = userRepository.findByEmail(Email.from(request.getEmail()))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 재검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 🔥 도메인 로직
        user.cancelWithdrawal(LocalDateTime.now(clock));

        // Redis 캐시 제거
        eventPublisher.publishEvent(UserAccountChangedEvent.profileUpdated(user.getId()));

        // 토큰 생성
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        redisTokenService.saveRefreshTokenHash(
                user.getId(),
                refreshToken,
                jwtProvider.getRemainingValidityMillis(refreshToken)
        );

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    @Override
    public void logout(String accessToken) {
        Claims claims;
        try {
            claims = jwtProvider.parseClaimsSafely(accessToken);
        } catch (JwtAuthenticationException e) {
            if (e.getErrorCode() == ErrorCode.EXPIRED_ACCESS_TOKEN) {
                return;
            }
            throw e;
        }

        Long userId = Long.valueOf(claims.getSubject());

        redisTokenService.deleteRefreshTokenHash(userId);

        long ttl = jwtProvider.getRemainingValidityMillis(accessToken);
        if (ttl > 0) {
            redisTokenService.blacklistAccessToken(accessToken, ttl);
        }
    }
}
