package com.example.my_project_1.auth.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isExpired(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getClaim(token, "typ"));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getClaim(token, "typ"));
    }

    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        Object uid = parseClaims(token).get("uid");
        return uid != null ? Long.valueOf(uid.toString()) : null;
    }

    public String getRole(String token) {
        Object role = parseClaims(token).get("role");
        return role != null ? role.toString() : null;
    }

    public long getRemainingValidityMillis(String token) {
        try {
            Date exp = parseClaims(token).getExpiration();
            return Math.max(0, exp.getTime() - System.currentTimeMillis());
        } catch (JwtException e) {
            return 0;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String getClaim(String token, String key) {
        Object value = parseClaims(token).get(key);
        return value != null ? value.toString() : null;
    }
}
