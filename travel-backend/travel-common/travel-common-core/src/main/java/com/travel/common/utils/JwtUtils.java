package com.travel.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared JWT utility used by gateway and services.
 */
public class JwtUtils {

    private static String secret;
    private static long expiration;

    private JwtUtils() {
    }

    private static SecretKey getSigningKey() {
        if (!hasText(secret)) {
            throw new IllegalStateException("JWT签名密钥尚未初始化");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static void configure(String jwtSecret, long expirationMillis) {
        if (!hasText(jwtSecret)) {
            throw new IllegalArgumentException("JWT签名密钥不能为空");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT签名密钥长度不能小于32字节");
        }
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("JWT过期时间必须大于0");
        }
        secret = jwtSecret;
        expiration = expirationMillis;
    }

    public static String generateToken(Long userId, String username, String roles) {
        return generateToken(userId, username, roles, newLoginSessionId());
    }

    public static String generateToken(Long userId, String username, String roles, String loginSessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roles", roles);
        claims.put("loginSessionId", hasText(loginSessionId) ? loginSessionId : newLoginSessionId());

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static long getRemainingMillis(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return 0;
        }
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }

    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    public static String getRoles(String token) {
        Claims claims = parseToken(token);
        return claims.get("roles", String.class);
    }

    public static String getLoginSessionId(String token) {
        Claims claims = parseToken(token);
        return resolveLoginSessionId(claims);
    }

    public static String resolveLoginSessionId(Claims claims) {
        String loginSessionId = claims.get("loginSessionId", String.class);
        if (hasText(loginSessionId)) {
            return loginSessionId;
        }
        Date issuedAt = claims.getIssuedAt();
        String subject = claims.getSubject();
        if (hasText(subject) && issuedAt != null) {
            return "legacy-" + subject + "-" + issuedAt.getTime();
        }
        return null;
    }

    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public static String newLoginSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
