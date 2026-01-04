package com.example.my_project_1.auth.utils;

import com.example.my_project_1.auth.exception.JwtAuthenticationException;
import com.example.my_project_1.common.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

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

    public String createAccessToken(Long userId, String email, String role) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(email)
                .addClaims(Map.of(
                        "uid", userId,
                        "role", role,
                        "typ", "access"
                ))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + properties.getAccessTokenExpiration()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String email) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(email)
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
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }
    }

    public void validateAccessToken(Claims claims) {
        if (!"access".equals(claims.get("typ"))) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        Date exp = claims.getExpiration();
        if (exp.before(new Date())) {
            throw new JwtAuthenticationException(ErrorCode.EXPIRED_ACCESS_TOKEN);
        }
    }

    public void validateRefreshToken(Claims claims) {
        if (!"refresh".equals(claims.get("typ"))) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Date exp = claims.getExpiration();
        if (exp.before(new Date())) {
            throw new JwtAuthenticationException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }
    }

    public long getRemainingValidityMillis(String token) {
        try {
            Claims claims = parseClaimsSafely(token);
            return Math.max(0, claims.getExpiration().getTime() - System.currentTimeMillis());
        } catch (Exception e) {
            return 0;
        }
    }
}
