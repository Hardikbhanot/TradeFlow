package com.tradeflow.market_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@EnableScheduling
@Slf4j
public class DashboardPublisher {

    private final MarketService marketService;
    private final SimpMessagingTemplate messagingTemplate;

    // Curated list of top active Indian stocks for the live dashboard
    private static final List<String> TOP_STOCKS = List.of(
            "RELIANCE", "TCS", "HDFCBANK", "ICICIBANK", "INFY", "ITC",
            "SBIN", "BHARTIARTL", "KOTAKBANK", "LT");

    private Map<String, BigDecimal> lastKnownPrices = null;

    public DashboardPublisher(MarketService marketService, SimpMessagingTemplate messagingTemplate) {
        this.marketService = marketService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Runs every 2 seconds (2000 ms).
     * Fetches the live batch of prices and pushes them to the connected STOMP
     * WebSocket clients.
     */
    @Scheduled(fixedRate = 2000)
    public void publishLiveMarketData() {
        try {
            // IF the market is closed AND we already fetched the closing prices once,
            // just broadcast the cached prices to accommodate new frontend connections
            // without wasting Upstox API calls.
            if (!marketService.isMarketOpen() && lastKnownPrices != null) {
                messagingTemplate.convertAndSend("/topic/market-data", lastKnownPrices);
                return; // Skip the external API call
            }

            Long startTime = System.currentTimeMillis();

            // 1. Fetch live prices for all TOP_STOCKS efficiently
            Map<String, BigDecimal> livePrices = marketService.getBatchLivePrices(TOP_STOCKS);

            // 2. Broadcast the Map to all frontend clients subscribed to /topic/market-data
            if (livePrices != null && !livePrices.isEmpty()) {
                lastKnownPrices = livePrices; // Cache it for market-closed fallback
                messagingTemplate.convertAndSend("/topic/market-data", livePrices);

                Long duration = System.currentTimeMillis() - startTime;
                log.info("📡 Pushed {} live prices to Dashboard WS in {}ms", livePrices.size(), duration);
            }
        } catch (Exception e) {
            log.error("❌ Failed to push market data to WebSocket: {}", e.getMessage(), e);
        }
    }
}
