package com.example.my_project_1.auth.utils;

import com.example.my_project_1.auth.exception.JwtAuthenticationException;
import com.example.my_project_1.common.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties properties;
    private Key key;

    @PostConstruct
    void init() {
        if (properties.getSecret().length() < 32) {
            throw new IllegalStateException("jwt secret must be at least 256bits");
        }
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes());
    }

    public String createAccessToken(Long userId, String role) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .addClaims(Map.of(
                        "role", role,
                        "typ", "access"
                ))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + properties.getAccessTokenExpiration()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("typ", "refresh")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + properties.getRefreshTokenExpiration()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    public Claims parseClaimsSafely(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            String type = e.getClaims().get("typ", String.class);
            if ("refresh".equals(type)) {
                throw new JwtAuthenticationException(ErrorCode.EXPIRED_REFRESH_TOKEN);
            }
            if ("access".equals(type)) {
                throw new JwtAuthenticationException(ErrorCode.EXPIRED_ACCESS_TOKEN);
            }
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);

        } catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            log.error("[JwtProvider.parseClaimsSafely]: Unknown error: {}", e.getMessage());
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }
    }

    public void assertAccessToken(Claims claims) {
        if (!"access".equals(claims.get("typ"))) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }

    public void assertRefreshToken(Claims claims) {
        if (!"refresh".equals(claims.get("typ"))) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    public long getRemainingValidityMillis(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            return claims.getExpiration().getTime() - System.currentTimeMillis();
        } catch (ExpiredJwtException e) {
            return 0;
        } catch (Exception e) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }
    }
}
