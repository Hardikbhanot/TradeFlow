package com.tradeflow.auth_service.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

@RestController
@RequestMapping({"/auth/indmoney", "/auth/upstox"})
@Slf4j
public class IndMoneyAuthController {

    @Value("${indmoney.api.key:wxyz123}")
    private String apiKey;

    @Value("${indmoney.api.secret:}")
    private String apiSecret;

    private final StringRedisTemplate redisTemplate;
    public static final String REDIS_TOKEN_KEY = "indmoney:access_token";

    public IndMoneyAuthController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBrokerStatus() {
        // Check if we have a fresh token in Redis, or fallback to properties secret
        String activeToken = redisTemplate.opsForValue().get(REDIS_TOKEN_KEY);
        boolean isConnected = (activeToken != null && !activeToken.isBlank()) 
                           || (apiSecret != null && !apiSecret.isBlank() && !apiSecret.contains("your_generated_access_token"));
        
        return ResponseEntity.ok(Map.of(
            "connected", isConnected,
            "broker", "INDstocks",
            "mode", "Static Bearer Token",
            "ipWhitelisted", true,
            "ipAddress", "139.59.25.118"
        ));
    }

    @PostMapping("/token")
    public ResponseEntity<?> updateMorningToken(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token cannot be empty"));
        }

        // Calculate TTL until 6:00 AM next morning in India timezone (IST)
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime now = ZonedDateTime.now(istZone);
        ZonedDateTime next6AM = now.with(LocalTime.of(6, 0, 0));
        if (now.isAfter(next6AM)) {
            next6AM = next6AM.plusDays(1);
        }
        Duration ttl = Duration.between(now, next6AM);

        // Store in Redis with TTL so it auto-expires exactly at 6:00 AM!
        redisTemplate.opsForValue().set(REDIS_TOKEN_KEY, token, ttl);
        redisTemplate.opsForValue().set("upstox:access_token", token, ttl); // Backward-compatibility fallback
        
        log.info("✅ Fresh INDmoney morning token saved to Redis! Active until 6:00 AM IST next morning. TTL: {} hours", ttl.toHours());
        
        return ResponseEntity.ok(Map.of(
            "message", "Token successfully configured",
            "activeUntil", next6AM.toString(),
            "ttlHours", ttl.toHours()
        ));
    }

    @GetMapping("/login-url")
    public ResponseEntity<Map<String, String>> getLoginUrl() {
        return ResponseEntity.ok(Map.of("url", "https://www.indstocks.com/app/api-trading"));
    }
}
