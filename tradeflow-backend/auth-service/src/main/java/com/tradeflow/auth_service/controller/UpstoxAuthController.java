package com.tradeflow.auth_service.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth/upstox")
@Slf4j
public class UpstoxAuthController {

    @Value("${upstox.api.key}")
    private String apiKey;

    @Value("${upstox.api.secret}")
    private String apiSecret;

    @Value("${upstox.redirect.uri}")
    private String redirectUri;

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    public static final String REDIS_TOKEN_KEY = "upstox:access_token";

    public UpstoxAuthController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Handles the redirect from Upstox after the user logs in.
     * Request URL: /auth/upstox/callback?code=XXXX
     */
    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestParam String code) {
        log.info("📥 Received Upstox Auth Code. Attempting token exchange...");

        try {
            String tokenUrl = "https://api.upstox.com/v2/login/authorization/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("code", code);
            body.add("client_id", apiKey);
            body.add("client_secret", apiSecret);
            body.add("redirect_uri", redirectUri);
            body.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<com.fasterxml.jackson.databind.JsonNode> response = restTemplate.postForEntity(tokenUrl, entity, com.fasterxml.jackson.databind.JsonNode.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                com.fasterxml.jackson.databind.JsonNode responseBody = response.getBody();
                if (responseBody.has("access_token")) {
                    String accessToken = responseBody.get("access_token").asText();
                    
                    // Save to Redis with 24h TTL
                    redisTemplate.opsForValue().set(REDIS_TOKEN_KEY, accessToken, Duration.ofHours(24));
                    log.info("✅ Upstox Access Token successfully saved to Redis!");
                    return ResponseEntity.ok("Upstox Connection Successful! You can close this window. Token saved for 24 hours.");
                }
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to exchange code for token: " + response.getBody());

        } catch (Exception e) {
            log.error("❌ Upstox Token Exchange Failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during Upstox connection: " + e.getMessage());
        }
    }
}
