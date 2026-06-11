package com.knatware.aiworkforce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues and validates JWT access tokens. The signing secret is configurable
 * via {@code app.jwt.secret}; a development default is provided so the app runs
 * out of the box (override it in production).
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expiryMillis;

    public JwtService(
            @Value("${app.jwt.secret:change-me-in-production-please-use-a-long-random-secret-string}") String secret,
            @Value("${app.jwt.expiry-minutes:120}") long expiryMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMillis = expiryMinutes * 60_000;
    }

    public String issue(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMillis))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String username(String token) { return parse(token).getSubject(); }
    public String role(String token) { return parse(token).get("role", String.class); }

    public boolean isValid(String token) {
        try { parse(token); return true; }
        catch (Exception e) { return false; }
    }
}
