package com.tradeflow.auth_service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    // IMPORTANT: In production, this should be an environment variable.
    // Must be at least 256 bits (32 characters) for HS256
    private static final String SECRET_KEY_STRING = "TradeFlowSuperSecretKeyForJwtAuthentication2026!";
    private final SecretKey secretKey = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());

    // 24 Hours in milliseconds
    private static final long EXPIRATION_TIME = 86400000;

    public String generateToken(String userId, String username, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("name", username); // As requested in the structure
        claims.put("email", email);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId) // "sub": "1"
                .setIssuedAt(new Date(System.currentTimeMillis())) // "iat"
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // "exp"
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
