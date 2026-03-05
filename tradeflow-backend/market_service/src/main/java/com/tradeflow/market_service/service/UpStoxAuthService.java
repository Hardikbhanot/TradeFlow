package com.tradeflow.market_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

@Service
@Slf4j
public class UpStoxAuthService {

    @Value("${upstox.api.key}")
    private String apiKey;

    @Value("${upstox.api.secret}")
    private String apiSecret;

    @Value("${upstox.redirect.uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAccessToken(String authCode) {
        String url = "https://api.upstox.com/v2/login/authorization/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("accept", "application/json");
        headers.set("Api-Version", "2.0");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", authCode);
        body.add("client_id", apiKey);
        body.add("client_secret", apiSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String accessToken = response.getBody().get("access_token").asText();
                log.info("✅ Successfully generated Upstox Access Token: {}", accessToken);
                return accessToken;
            }
        } catch (Exception e) {
            log.error("❌ Failed to get Upstox Access Token: {}", e.getMessage());
        }
        return null;
    }

    public String getLoginUrl() {
        return "https://api.upstox.com/v2/login/authorization/dialog?response_type=code&client_id="
                + apiKey + "&redirect_uri=" + redirectUri;
    }
}
