package com.tradeflow.market_service.service;

import yahoofinance.YahooFinance;
import yahoofinance.Stock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.math.RoundingMode;

@Service
@Slf4j
public class MarketService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Set<LocalDate> NSE_HOLIDAYS = Set.of(
        LocalDate.of(2026, 1, 26), // Republic Day
        LocalDate.of(2026, 3, 3),  // Holi (TODAY!)
        LocalDate.of(2026, 3, 26), // Ram Navami
        LocalDate.of(2026, 3, 31)  // Mahavir Jayanti
    );
    private static final String CACHE_KEY_PREFIX = "price:";

    public MarketService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public BigDecimal getLivePrice(String symbol) {
        String cacheKey = CACHE_KEY_PREFIX + symbol;

        // 1. Check Redis Cache first
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            return new BigDecimal(cachedValue.toString());
        }

        try {
            // 2. Try to fetch real data from Yahoo
             if (!isMarketOpen()) {
                    price = getMockPrice(price); 
                    return price;
                }
            Stock stock = YahooFinance.get(symbol);
            if (stock != null && stock.getQuote().getPrice() != null) {
                BigDecimal price = stock.getQuote().getPrice();
                redisTemplate.opsForValue().set(cacheKey, price.toString(), Duration.ofSeconds(10));
                return price;
            }
        } catch (Exception e) {
            log.error("❌ Yahoo API down/Limit reached. Falling back to full mock for {}", symbol);
        }

        // 3. Absolute Fallback if even Yahoo fails
        return getMockPrice(new BigDecimal("2500.00")); 
    }

    private BigDecimal getMockPrice(String symbol) {
    // Hardcoded "Base Prices" based on today's market close
    double basePrice = switch (symbol) {
        case "RELIANCE.NS" -> 2985.00;
        case "TCS.NS" -> 4120.00;
        case "INFY.NS" -> 1650.00;
        case "BEL.NS" -> 210.00;
        default -> 1000.00;
    };

    // Add 0.1% random wiggle so you can see the "Live" effect in Postman
    double fluctuation = 1 + (Math.random() * 0.002 - 0.001); 
    return BigDecimal.valueOf(basePrice * fluctuation).setScale(2, RoundingMode.HALF_UP);
}

    private boolean isMarketOpen() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        // 1. Check Weekend
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return false;

        // 2. Check Today's Date against Holiday List
        if (NSE_HOLIDAYS.contains(now.toLocalDate())) return false;

        // 3. Check Market Hours (9:15 AM to 3:30 PM)
        return !time.isBefore(LocalTime.of(9, 15)) && !time.isAfter(LocalTime.of(15, 30));
    }
}