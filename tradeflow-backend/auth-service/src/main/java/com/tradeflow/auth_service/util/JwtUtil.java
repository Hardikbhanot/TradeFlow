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

    private final SecretKey secretKey;

    // 30 Minutes in milliseconds
    private static final long EXPIRATION_TIME = 1800000;

    public JwtUtil(@org.springframework.beans.factory.annotation.Value("${jwt.secret:TradeFlowSuperSecretKeyForJwtAuthentication2026!}") String secretKeyString) {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
    }

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
