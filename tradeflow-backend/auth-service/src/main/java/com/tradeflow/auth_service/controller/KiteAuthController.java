package com.tradeflow.auth_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth/kite")
@Slf4j
public class KiteAuthController {

    static final String REDIS_TOKEN_KEY = "kite:access_token";
    static final String REDIS_TOKEN_EXPIRY_KEY = "kite:access_token:expires_at";
    private static final String STATE_KEY_PREFIX = "kite:oauth_state:";
    private static final String TOKEN_URL = "https://api.kite.trade/session/token";

    @Value("${kite.api.key:}")
    private String apiKey;

    @Value("${kite.api.secret:}")
    private String apiSecret;

    @Value("${kite.redirect.uri:https://api.tradeflow.hbhanot.tech/auth/kite/callback}")
    private String redirectUri;

    @Value("${kite.frontend.redirect-url:https://tradeflow.hbhanot.tech/dashboard}")
    private String frontendRedirectUrl;

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KiteAuthController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        String accessToken = redisTemplate.opsForValue().get(REDIS_TOKEN_KEY);
        boolean connected = accessToken != null && !accessToken.isBlank();
        if (connected) {
            String expiryTimestamp = redisTemplate.opsForValue().get(REDIS_TOKEN_EXPIRY_KEY);
            connected = expiryTimestamp != null
                    && Long.parseLong(expiryTimestamp) > System.currentTimeMillis();
            if (!connected) {
                redisTemplate.delete(REDIS_TOKEN_KEY);
                redisTemplate.delete(REDIS_TOKEN_EXPIRY_KEY);
            }
        }
        return ResponseEntity.ok(Map.of(
                "connected", connected,
                "broker", "Zerodha Kite",
                "expiresIn", connected ? "Daily session; reconnect when expired" : "N/A"));
    }

    @GetMapping("/login-url")
    public ResponseEntity<?> getLoginUrl() {
        if (apiKey.isBlank() || apiSecret.isBlank() || redirectUri.isBlank()) {
            log.error("Kite API credentials or redirect URI are missing: apiKey present={}, apiSecret present={}, redirectUri={}",
                    !apiKey.isBlank(), !apiSecret.isBlank(), redirectUri);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Kite API key and redirect URL are not configured on the backend."));
        }

        String state = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(STATE_KEY_PREFIX + state, "active", Duration.ofMinutes(10));

        String url = "https://kite.zerodha.com/connect/login?v=3&api_key="
                + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&redirect_params="
                + URLEncoder.encode("state=" + state, StandardCharsets.UTF_8);
        log.info("Generated Kite login URL: {}", url);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String request_token,
            @RequestParam(required = false) String state) {
        log.info("Received Kite login callback: status={}, request_token={}, state={}",
                status, request_token != null ? "[PRESENT]" : "[NULL]", state);

        if (!"success".equalsIgnoreCase(status) || request_token == null || request_token.isBlank()) {
            log.warn("Kite login callback failed or missing request_token: status={}", status);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Kite login was not completed successfully."));
        }

        if (state == null || !Boolean.TRUE.equals(redisTemplate.hasKey(STATE_KEY_PREFIX + state))) {
            log.warn("Kite callback state validation failed for state={}", state);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Invalid or expired Kite login state. Please reconnect."));
        }
        redisTemplate.delete(STATE_KEY_PREFIX + state);

        if (apiKey.isBlank() || apiSecret.isBlank()) {
            log.error("Kite API credentials missing during token exchange.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Kite API credentials are not configured on the backend."));
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("api_key", apiKey);
            form.add("request_token", request_token);
            form.add("checksum", createChecksum(apiKey + request_token + apiSecret));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("X-Kite-Version", "3");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    TOKEN_URL, new HttpEntity<>(form, headers), String.class);
            if (response.getBody() == null) {
                log.error("Kite token exchange returned null body.");
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("message", "Kite returned an empty token response."));
            }
            JsonNode accessTokenNode = objectMapper.readTree(response.getBody()).path("data").path("access_token");
            if (!response.getStatusCode().is2xxSuccessful()
                    || accessTokenNode.isMissingNode()
                    || accessTokenNode.asText().isBlank()) {
                log.error("Kite token exchange failed: HTTP {}, body={}", response.getStatusCode(), response.getBody());
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("message", "Kite did not return an access token."));
            }

            Duration lifetime = tokenLifetime();
            long expiryTimestamp = System.currentTimeMillis() + lifetime.toMillis();
            redisTemplate.opsForValue().set(REDIS_TOKEN_KEY, accessTokenNode.asText(), lifetime);
            redisTemplate.opsForValue().set(
                    REDIS_TOKEN_EXPIRY_KEY, Long.toString(expiryTimestamp), lifetime);
            log.info("Kite access token saved to Redis with lifetime {} minutes.", lifetime.toMinutes());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendRedirectUrl))
                    .build();
        } catch (RestClientException | java.io.IOException e) {
            log.error("Kite token exchange exception: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "Kite token exchange failed. Please reconnect."));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for Kite token exchange.", e);
        }
    }

    private String createChecksum(String value) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private Duration tokenLifetime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        ZonedDateTime expiry = now.toLocalDate().plusDays(1)
                .atTime(LocalTime.of(6, 0))
                .atZone(now.getZone());
        return Duration.between(now, expiry);
    }
}
