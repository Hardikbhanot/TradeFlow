package com.tradeflow.market_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tradeflow.market_service.dto.PriceHistoryPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

@Service
@Slf4j
public class MarketService {

    @Value("${upstox.access.token}")
    private String accessToken;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String CACHE_KEY_PREFIX = "price:";
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter INTRADAY_LABEL_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAILY_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd MMM");

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
            
            // Check Redis first for a fresh token
            String activeToken = (String) redisTemplate.opsForValue().get("upstox:access_token");
            if (activeToken == null) {
                log.debug("No Redis token found, falling back to ENV token.");
                activeToken = accessToken;
            }

            headers.set("Authorization", "Bearer " + activeToken);
            headers.set("Accept", "application/json");
            headers.set("Api-Version", "2.0");

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
    public Map<String, BigDecimal> getBatchLivePrices(List<String> symbols) {
        Map<String, BigDecimal> results = new java.util.HashMap<>();
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
            
            // Check Redis first for a fresh token
            String activeToken = (String) redisTemplate.opsForValue().get("upstox:access_token");
            if (activeToken == null) {
                activeToken = accessToken;
            }

            headers.set("Authorization", "Bearer " + activeToken);
            headers.set("Accept", "application/json");
            headers.set("Api-Version", "2.0");

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

        // Guarantee every requested symbol is present for frontend consumers.
        for (String symbol : symbols) {
            results.computeIfAbsent(symbol, this::getLivePrice);
        }

        return results;
    }

    public List<PriceHistoryPoint> getPriceHistory(String symbol, String range) {
        String normalizedRange = range == null ? "TODAY" : range.trim().toUpperCase();
        return switch (normalizedRange) {
            case "30D", "1M", "30" -> buildDailyHistory(symbol, 30);
            case "60D", "2M", "60" -> buildDailyHistory(symbol, 60);
            case "TODAY", "1D", "INTRADAY" -> buildIntradayHistory(symbol);
            default -> buildIntradayHistory(symbol);
        };
    }

    private List<PriceHistoryPoint> buildIntradayHistory(String symbol) {
        ZonedDateTime now = ZonedDateTime.now(IST);
        LocalDate today = now.toLocalDate();

        ZonedDateTime start = today.atTime(9, 15).atZone(IST);
        ZonedDateTime close = today.atTime(15, 30).atZone(IST);

        ZonedDateTime end;
        if (!isTradingDay(today)) {
            end = close;
        } else if (now.isBefore(start)) {
            end = start;
        } else if (now.isAfter(close)) {
            end = close;
        } else {
            end = now.withSecond(0).withNano(0);
        }

        List<ZonedDateTime> timestamps = new ArrayList<>();
        ZonedDateTime cursor = start;
        while (!cursor.isAfter(end)) {
            timestamps.add(cursor);
            cursor = cursor.plusMinutes(15);
        }

        if (timestamps.size() < 2) {
            timestamps.add(end.plusMinutes(15));
        }

        BigDecimal latest = getLivePrice(symbol);
        List<BigDecimal> series = generateSeries(
                latest,
                timestamps.size(),
                0.0045,
                Objects.hash(symbol.toUpperCase(), "TODAY", today.toString()));

        List<PriceHistoryPoint> points = new ArrayList<>(timestamps.size());
        for (int i = 0; i < timestamps.size(); i++) {
            points.add(new PriceHistoryPoint(timestamps.get(i).format(INTRADAY_LABEL_FORMAT), series.get(i)));
        }
        return points;
    }

    private List<PriceHistoryPoint> buildDailyHistory(String symbol, int days) {
        List<LocalDate> tradingDays = recentTradingDays(days);
        BigDecimal latest = getLivePrice(symbol);

        List<BigDecimal> series = generateSeries(
                latest,
                tradingDays.size(),
                0.0125,
                Objects.hash(symbol.toUpperCase(), "DAILY", days, LocalDate.now(IST).toString()));

        List<PriceHistoryPoint> points = new ArrayList<>(tradingDays.size());
        for (int i = 0; i < tradingDays.size(); i++) {
            points.add(new PriceHistoryPoint(tradingDays.get(i).format(DAILY_LABEL_FORMAT), series.get(i)));
        }
        return points;
    }

    private List<BigDecimal> generateSeries(BigDecimal latest, int points, double stepVolatility, int seed) {
        if (points <= 0) {
            return List.of();
        }
        if (points == 1) {
            return List.of(latest.setScale(2, RoundingMode.HALF_UP));
        }

        Random random = new Random(seed);
        List<Double> raw = new ArrayList<>(points);
        double value = 1.0;

        for (int i = 0; i < points; i++) {
            if (i > 0) {
                double shock = (random.nextDouble() * 2.0 - 1.0) * stepVolatility;
                value = Math.max(0.1, value * (1.0 + shock));
            }
            raw.add(value);
        }

        BigDecimal finalRaw = BigDecimal.valueOf(raw.get(points - 1));
        BigDecimal scale = latest.divide(finalRaw, 8, RoundingMode.HALF_UP);
        List<BigDecimal> out = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            BigDecimal point = BigDecimal.valueOf(raw.get(i))
                    .multiply(scale)
                    .setScale(2, RoundingMode.HALF_UP);
            out.add(point.max(BigDecimal.valueOf(0.01)));
        }
        return out;
    }

    private List<LocalDate> recentTradingDays(int count) {
        List<LocalDate> days = new ArrayList<>(count);
        LocalDate cursor = LocalDate.now(IST);

        while (days.size() < count) {
            if (isTradingDay(cursor)) {
                days.add(cursor);
            }
            cursor = cursor.minusDays(1);
        }

        Collections.reverse(days);
        return days;
    }

    private boolean isTradingDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        return !NSE_HOLIDAYS.contains(date);
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
        LocalDateTime now = LocalDateTime.now(IST);
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
            return false;
        if (NSE_HOLIDAYS.contains(now.toLocalDate()))
            return false;

        return !time.isBefore(LocalTime.of(9, 15)) && !time.isAfter(LocalTime.of(15, 30));
    }

    /**
     * Provides extended data for AI reporting (LTP + Simulated Prev Close)
     */
    public Map<String, Object> getExtendedPriceData(String symbol) {
        BigDecimal ltp = getLivePrice(symbol);
        // Simulate a previous close slightly different from the live price
        BigDecimal prevClose = ltp.multiply(BigDecimal.valueOf(0.985 + (Math.random() * 0.03)))
                .setScale(2, RoundingMode.HALF_UP);
        
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("symbol", symbol.toUpperCase());
        data.put("ltp", ltp);
        data.put("prevClose", prevClose);
        data.put("change", ltp.subtract(prevClose));
        data.put("changePercent", ltp.subtract(prevClose).divide(prevClose, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
        
        return data;
    }
}
