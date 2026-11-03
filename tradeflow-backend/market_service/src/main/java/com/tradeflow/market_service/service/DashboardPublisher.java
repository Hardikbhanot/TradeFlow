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
    private final KiteTickerService kiteTickerService;

    private Map<String, BigDecimal> lastKnownPrices = null;

    public DashboardPublisher(
            MarketService marketService,
            SimpMessagingTemplate messagingTemplate,
            KiteTickerService kiteTickerService) {
        this.marketService = marketService;
        this.messagingTemplate = messagingTemplate;
        this.kiteTickerService = kiteTickerService;
    }

    /**
     * Runs every 2 seconds (2000 ms).
     * Fetches the live batch of prices and pushes them to the connected STOMP
     * WebSocket clients.
     */
    @Scheduled(fixedRate = 2000)
    public void publishLiveMarketData() {
        // Keep the existing Upstox polling path as a fallback until the Kite stream is connected.
        if (kiteTickerService.isConnected()) {
            return;
        }

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
            Map<String, BigDecimal> livePrices =
                    marketService.getBatchLivePrices(KiteTickerService.SUBSCRIBED_SYMBOLS);

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
