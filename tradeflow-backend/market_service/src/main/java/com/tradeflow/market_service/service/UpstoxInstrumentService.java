package com.tradeflow.market_service.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

@Service
@Slf4j
public class UpstoxInstrumentService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CSV_URL = "https://assets.upstox.com/market-quote/instruments/exchange/NSE.csv.gz";
    private static final String INSTRUMENT_SEARCH_URL = "https://api.upstox.com/v2/instruments/search?query=";
    public static final String REDIS_HASH_KEY = "market:instruments";
    public static final String EQUITY_SET_KEY = "market:equity_symbols";

    public UpstoxInstrumentService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Value("${upstox.access.token:}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Resolves a symbol from Redis first and lazily learns symbols that were not
     * present in the startup instrument cache from Upstox.
     */
    public Optional<String> resolveInstrumentKey(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }

        String normalizedSymbol = symbol.trim().toUpperCase();
        Object cachedKey = redisTemplate.opsForHash().get(REDIS_HASH_KEY, normalizedSymbol);
        if (cachedKey != null && !cachedKey.toString().isBlank()) {
            return Optional.of(cachedKey.toString());
        }

        try {
            String encodedQuery = java.net.URLEncoder.encode(normalizedSymbol,
                    java.nio.charset.StandardCharsets.UTF_8.toString());
            HttpHeaders headers = new HttpHeaders();
            String activeToken = (String) redisTemplate.opsForValue().get("upstox:access_token");
            if (activeToken == null || activeToken.isBlank()) {
                activeToken = accessToken;
            }
            if (activeToken != null && !activeToken.isBlank()
                    && !activeToken.toLowerCase().startsWith("bearer ")) {
                activeToken = "Bearer " + activeToken;
            }
            headers.set("Authorization", activeToken);
            headers.set("Accept", "application/json");
            headers.set("Api-Version", "2.0");

            ResponseEntity<String> response = restTemplate.exchange(
                    INSTRUMENT_SEARCH_URL + encodedQuery,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode data = objectMapper.readTree(response.getBody()).path("data");
                for (JsonNode instrument : data) {
                    String tradingSymbol = instrument.path("trading_symbol").asText();
                    String instrumentKey = instrument.path("instrument_key").asText();
                    if (normalizedSymbol.equalsIgnoreCase(tradingSymbol) && !instrumentKey.isBlank()) {
                        cacheInstrument(normalizedSymbol, instrumentKey);
                        return Optional.of(instrumentKey);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Unable to resolve {} through Upstox instrument search: {}", normalizedSymbol,
                    e.getMessage());
        }

        return Optional.empty();
    }

    private void cacheInstrument(String symbol, String instrumentKey) {
        redisTemplate.opsForHash().put(REDIS_HASH_KEY, symbol, instrumentKey);
        if (instrumentKey.startsWith("NSE_EQ|")) {
            redisTemplate.opsForSet().add(EQUITY_SET_KEY, symbol);
        }
        log.info("Cached dynamically resolved instrument {} -> {}", symbol, instrumentKey);
    }

    /**
     * Executes immediately after Spring Boot starts up.
     * Downloads the Upstox NSE CSV master file, unzips it in memory,
     * and maps Every NSE Trading Symbol to its ISIN Instrument Key in Redis.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void fetchAndCacheInstruments() {
        log.info("🚀 Starting download of Upstox NSE Instruments CSV...");

        try {
            URL url = new URL(CSV_URL);

            // Unzip the .gz stream on the fly
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(url.openStream());
                    Reader reader = new InputStreamReader(gzipInputStream);) {

                // Parse the CSV
                Iterable<CSVRecord> records = CSVFormat.DEFAULT
                        .builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .build()
                        .parse(reader);

                Map<String, String> instrumentMap = new HashMap<>();
                int count = 0;

                for (CSVRecord record : records) {
                    // Based on Upstox Docs:
                    // instrument_key is typically column 0 (e.g. NSE_EQ|INE002A01018)
                    // tradingsymbol is typically column 2 (e.g. RELIANCE)
                    String instrumentKey = record.get("instrument_key");
                    String tradingSymbol = record.get("tradingsymbol");

                    if (instrumentKey != null && tradingSymbol != null) {
                        String symbol = tradingSymbol.toUpperCase();
                        instrumentMap.put(symbol, instrumentKey);
                        
                        // If it's pure NSE Equity, add to our quick-search Set
                        if (instrumentKey.startsWith("NSE_EQ|")) {
                            redisTemplate.opsForSet().add(EQUITY_SET_KEY, symbol);
                        }
                        count++;
                    }

                    // Flush to Redis in batches of 10,000 to prevent memory pressure
                    if (count % 10000 == 0) {
                        redisTemplate.opsForHash().putAll(REDIS_HASH_KEY, instrumentMap);
                        instrumentMap.clear();
                        log.info("📥 Loaded {} instruments into Redis...", count);
                    }
                }

                // Flush remaining
                if (!instrumentMap.isEmpty()) {
                    redisTemplate.opsForHash().putAll(REDIS_HASH_KEY, instrumentMap);
                    log.info("📥 Loaded final batch. Total instruments: {}", count);
                }

                log.info("✅ Successfully cached all Upstox NSE symbols to Redis!");
            }
        } catch (Exception e) {
            log.error("❌ Failed to fetch or parse Upstox CSV: {}", e.getMessage(), e);
        }
    }

    public List<String> searchInstruments(String query) {
        if (query == null || query.length() < 2) {
            return new ArrayList<>();
        }

        String searchPattern = query.toUpperCase() + "*";
        List<String> results = new ArrayList<>();

        // 1. Search in Equity symbols first (Highest priority)
        ScanOptions equityOptions = ScanOptions.scanOptions().match(searchPattern).count(500).build();
        try (Cursor<Object> cursor = redisTemplate.opsForSet().scan(EQUITY_SET_KEY, equityOptions)) {
            while (cursor.hasNext() && results.size() < 15) {
                results.add((String) cursor.next());
            }
        } catch (Exception e) {
            log.error("Error searching equity symbols: {}", e.getMessage());
        }

        // 2. If we don't have enough results, check the full instrument hash (e.g. for FOs/Options)
        if (results.size() < 10) {
            ScanOptions fullOptions = ScanOptions.scanOptions().match(searchPattern).count(500).build();
            try (Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan(REDIS_HASH_KEY, fullOptions)) {
                while (cursor.hasNext() && results.size() < 25) {
                    String key = (String) cursor.next().getKey();
                    if (!results.contains(key)) {
                        results.add(key);
                    }
                }
            } catch (Exception e) {
                log.error("Error searching full instruments hash: {}", e.getMessage());
            }
        }

        // 3. Sort by length then alphabetical to keep short symbols (base stocks) at top
        results.sort((a, b) -> {
            if (a.length() != b.length()) return a.length() - b.length();
            return a.compareTo(b);
        });

        return results.stream().limit(15).collect(Collectors.toList());
    }
}
