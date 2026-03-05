package com.tradeflow.market_service.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.Map;
import java.util.HashMap;

@Service
@Slf4j
public class UpstoxInstrumentService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CSV_URL = "https://assets.upstox.com/market-quote/instruments/exchange/NSE.csv.gz";
    public static final String REDIS_HASH_KEY = "market:instruments";

    public UpstoxInstrumentService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
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
                        instrumentMap.put(tradingSymbol.toUpperCase(), instrumentKey);
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
}
