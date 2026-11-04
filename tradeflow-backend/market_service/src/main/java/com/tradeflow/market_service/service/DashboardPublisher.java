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

    @Scheduled(fixedDelay = 5_000, initialDelay = 1_000)
    public void publishLiveMarketData() {
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

            // Fall back to the quote API when Kite is unavailable.
            Map<String, BigDecimal> livePrices =
                    marketService.getBatchLivePrices(kiteTickerService.getRequestedSymbols());

            if (livePrices != null && !livePrices.isEmpty()) {
                lastKnownPrices = livePrices; // Cache it for market-closed fallback
                messagingTemplate.convertAndSend("/topic/market-data", livePrices);

                Long duration = System.currentTimeMillis() - startTime;
                log.info("Published {} fallback market prices to WebSocket in {}ms", livePrices.size(), duration);
            }
        } catch (Exception e) {
            log.error("❌ Failed to push market data to WebSocket: {}", e.getMessage(), e);
        }
    }
}
