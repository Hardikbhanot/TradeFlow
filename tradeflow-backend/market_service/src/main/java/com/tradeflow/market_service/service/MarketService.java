package com.tradeflow.market_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.Set;

@Service
@Slf4j
public class MarketService {

    @Value("${upstox.access.token}")
    private String accessToken;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String CACHE_KEY_PREFIX = "price:";

    private static final Set<LocalDate> NSE_HOLIDAYS = Set.of(
            LocalDate.of(2026, 1, 26), // Republic Day
            LocalDate.of(2026, 3, 3), // Holi
            LocalDate.of(2026, 3, 26), // Ram Navami
            LocalDate.of(2026, 3, 31) // Mahavir Jayanti
    );

    private final UpstoxInstrumentService upstoxInstrumentService;

    public MarketService(RedisTemplate<String, Object> redisTemplate, UpstoxInstrumentService upstoxInstrumentService) {
        this.redisTemplate = redisTemplate;
        this.upstoxInstrumentService = upstoxInstrumentService;
    }

    /**
     * Main entry point for fetching prices.
     * Logic: Redis Cache -> Upstox API -> Mock Fallback
     */
    private String resolveInstrumentKey(String symbol) {
        // If it already looks like a valid Upstox key with a pipe or colon, return it
        // as-is
        if (symbol.contains("|") || symbol.contains(":")) {
            return symbol;
        }

        // Query the Redis Hash populated during application startup
        Object cachedKey = redisTemplate.opsForHash().get(UpstoxInstrumentService.REDIS_HASH_KEY, symbol.toUpperCase());

        if (cachedKey != null) {
            log.debug("Found {} dynamically mapped to {}", symbol, cachedKey);
            return cachedKey.toString();
        }

        log.warn("Symbol {} not found in Redis mapping. Falling back to generic structure.", symbol);
        return "NSE_EQ|" + symbol.toUpperCase(); // Fallback structure
    }

    public BigDecimal getLivePrice(String symbol) {
        String instrumentKey = resolveInstrumentKey(symbol);
        String cacheKey = CACHE_KEY_PREFIX + symbol.toUpperCase();

        // 1. Check Redis Cache first (Stay efficient!)
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            return new BigDecimal(cachedValue.toString());
        }

        // 2. Try fetching from Upstox
        try {
            // Encode the newly mapped instrumentKey (handles the | character properly)
            String encodedSymbol = java.net.URLEncoder.encode(instrumentKey,
                    java.nio.charset.StandardCharsets.UTF_8.toString());
            java.net.URI uri = java.net.URI
                    .create("https://api.upstox.com/v2/market-quote/quotes?instrument_key=" + encodedSymbol);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode rootNode = mapper.readTree(response.getBody());
                JsonNode data = rootNode.get("data");

                // Navigate to the first instrument in the response
                if (data != null && data.fields().hasNext()) {
                    JsonNode stockData = data.fields().next().getValue();
                    BigDecimal price = stockData.get("last_price").decimalValue();

                    // Store in Redis for 5 seconds
                    redisTemplate.opsForValue().set(cacheKey, price.toString(), Duration.ofSeconds(5));

                    log.info("🎯 Upstox Live Price for {}: ₹{}", symbol, price);
                    return price;
                }
            }
        } catch (Exception e) {
            log.error("⚠️ Upstox API failed or unauthorized. Using Mock logic. Error: {}", e.getMessage());
        }

        // 3. Fallback to Simulated Mock Price
        return getMockPrice(symbol);
    }

    /**
     * Batch fetch live prices for the WebSocket Dashboard
     */
    public java.util.Map<String, BigDecimal> getBatchLivePrices(java.util.List<String> symbols) {
        java.util.Map<String, BigDecimal> results = new java.util.HashMap<>();
        if (symbols == null || symbols.isEmpty())
            return results;

        // Map symbols to instrument keys
        java.util.List<String> instrumentKeys = symbols.stream()
                .map(this::resolveInstrumentKey)
                .toList();

        // Join keys with commas for the Upstox API
        String joinedKeys = String.join(",", instrumentKeys);

        try {
            // Encode the joined string
            String encodedKeys = java.net.URLEncoder.encode(joinedKeys,
                    java.nio.charset.StandardCharsets.UTF_8.toString());
            java.net.URI uri = java.net.URI
                    .create("https://api.upstox.com/v2/market-quote/quotes?instrument_key=" + encodedKeys);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode rootNode = mapper.readTree(response.getBody());
                JsonNode data = rootNode.get("data");

                if (data != null) {
                    // Iterate over all instruments returned
                    java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = data.fields();
                    while (fields.hasNext()) {
                        java.util.Map.Entry<String, JsonNode> field = fields.next();
                        JsonNode stockData = field.getValue();
                        String mappedSymbol = stockData.get("symbol").asText();
                        BigDecimal price = stockData.get("last_price").decimalValue();

                        results.put(mappedSymbol, price);

                        // Also update the individual cache so regular REST calls benefit!
                        String cacheKey = CACHE_KEY_PREFIX + mappedSymbol.toUpperCase();
                        redisTemplate.opsForValue().set(cacheKey, price.toString(), Duration.ofSeconds(5));
                    }
                }
            }
        } catch (Exception e) {
            log.error("⚠️ Upstox Batch API failed. Error: {}", e.getMessage());
            // Fallback to mock prices for the batch safely
            for (String symbol : symbols) {
                results.put(symbol, getMockPrice(symbol));
            }
        }

        return results;
    }

    private BigDecimal getMockPrice(String symbol) {
        String cacheKey = CACHE_KEY_PREFIX + "mock:" + symbol;

        // Check if we have a simulated price in Redis already
        Object cachedMock = redisTemplate.opsForValue().get(cacheKey);
        BigDecimal base;

        if (cachedMock != null) {
            base = new BigDecimal(cachedMock.toString());
        } else {
            base = getBasePrice(symbol);
        }

        BigDecimal fluctuated = fluctuate(base);

        // Update the mock price in Redis for 5 seconds to create a "ticker" effect
        redisTemplate.opsForValue().set(cacheKey, fluctuated.toString(), Duration.ofSeconds(5));
        return fluctuated;
    }

    private BigDecimal getBasePrice(String symbol) {
        double price = switch (symbol.toUpperCase()) {
            case "RELIANCE", "NSE_EQ:RELIANCE" -> 2985.00;
            case "TCS", "NSE_EQ:TCS" -> 4120.00;
            case "INFY", "NSE_EQ:INFY" -> 1650.00;
            case "BEL", "NSE_EQ:BEL" -> 210.00;
            default -> 1000.00;
        };
        return BigDecimal.valueOf(price);
    }

    private BigDecimal fluctuate(BigDecimal price) {
        double changePercent = (Math.random() * 0.004) - 0.002; // -0.2% to +0.2% for more realism
        return price.add(price.multiply(BigDecimal.valueOf(changePercent)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isMarketOpen() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
            return false;
        if (NSE_HOLIDAYS.contains(now.toLocalDate()))
            return false;

        return !time.isBefore(LocalTime.of(9, 15)) && !time.isAfter(LocalTime.of(15, 30));
    }
}