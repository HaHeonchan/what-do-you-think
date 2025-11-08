package com.example.demo.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration:86400000}") // 24시간 (밀리초)
    private Long expiration;

    private SecretKey getSigningKey() {
        // JWT_SECRET이 환경 변수에서 설정되지 않았는지 확인
        if (secret == null || secret.isEmpty() || secret.startsWith("${")) {
            throw new IllegalStateException(
                "JWT_SECRET environment variable is required. " +
                "Please set JWT_SECRET in your .env file or as an environment variable with at least 32 characters."
            );
        }
        
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        
        // JWT HMAC-SHA 알고리즘은 최소 256비트(32바이트) 키가 필요
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                "JWT secret key must be at least 256 bits (32 bytes). " +
                "Current key length: " + keyBytes.length + " bytes. " +
                "Please set JWT_SECRET environment variable or in .env file with at least 32 characters."
            );
        }
        
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}

