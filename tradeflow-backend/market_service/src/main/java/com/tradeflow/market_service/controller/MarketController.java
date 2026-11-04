package com.tradeflow.market_service.controller;

import com.tradeflow.market_service.dto.PriceHistoryPoint;
import com.tradeflow.market_service.service.KiteTickerService;
import com.tradeflow.market_service.service.MarketService;
import com.tradeflow.market_service.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    private final MarketService marketService;
    private final NewsService newsService;
    private final com.tradeflow.market_service.service.UpStoxAuthService upStoxAuthService;
    private final com.tradeflow.market_service.service.UpstoxInstrumentService upstoxInstrumentService;
    private final KiteTickerService kiteTickerService;

    public MarketController(MarketService marketService,
            NewsService newsService,
            com.tradeflow.market_service.service.UpStoxAuthService upStoxAuthService,
            com.tradeflow.market_service.service.UpstoxInstrumentService upstoxInstrumentService,
            KiteTickerService kiteTickerService) {
        this.marketService = marketService;
        this.newsService = newsService;
        this.upStoxAuthService = upStoxAuthService;
        this.upstoxInstrumentService = upstoxInstrumentService;
        this.kiteTickerService = kiteTickerService;
    }

    @GetMapping("/news")
    public List<Map<String, String>> getNews() {
        return newsService.getLatestMarketNews();
    }

    @GetMapping("/price/{symbol}")
    public BigDecimal getPrice(@PathVariable String symbol) {
        return marketService.getLivePrice(symbol);
    }

    @GetMapping("/prices")
    public Map<String, BigDecimal> getPrices(@RequestParam("symbols") String symbols) {
        List<String> parsedSymbols = Arrays.stream(symbols.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return marketService.getBatchLivePrices(parsedSymbols);
    }

    @PostMapping("/subscriptions")
    public List<String> subscribeToLivePrices(@RequestBody Map<String, List<String>> request) {
        if (request == null || request.get("symbols") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A symbols list is required.");
        }
        return kiteTickerService.subscribeSymbols(request.get("symbols"));
    }

    @GetMapping("/stream/status")
    public Map<String, Object> getLiveStreamStatus() {
        return kiteTickerService.getStreamStatus();
    }

    @GetMapping("/history/{symbol}")
    public List<PriceHistoryPoint> getHistory(
            @PathVariable String symbol,
            @RequestParam(name = "range", defaultValue = "TODAY") String range) {
        return marketService.getPriceHistory(symbol, range);
    }

    @GetMapping("/search")
    public List<String> search(@RequestParam("q") String query) {
        return upstoxInstrumentService.searchInstruments(query);
    }


    @GetMapping("/login")
    public String getUpstoxLoginUrl() {
        return upStoxAuthService.getLoginUrl();
    }

    @GetMapping("/upstox/callback")
    public org.springframework.http.ResponseEntity<Map<String, Object>> handleUpstoxCallback(@RequestParam("code") String code) {
        String token = upStoxAuthService.getAccessToken(code);
        if (token != null) {
            return org.springframework.http.ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Broker authentication token generated and stored securely."
            ));
        } else {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "FAILED",
                    "message", "Failed to generate Upstox broker token."
            ));
        }
    }

    @GetMapping("/data/{symbol}")
    public Map<String, Object> getExtendedData(@PathVariable String symbol) {
        return marketService.getExtendedPriceData(symbol);
    }

    @GetMapping("/depth/{symbol}")
    public Map<String, Object> getMarketDepth(@PathVariable String symbol) {
        return marketService.getMarketDepth(symbol);
    }
}
