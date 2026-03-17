package com.tradeflow.market_service.controller;

import com.tradeflow.market_service.dto.PriceHistoryPoint;
import com.tradeflow.market_service.service.MarketService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    private final MarketService marketService;
    private final com.tradeflow.market_service.service.UpStoxAuthService upStoxAuthService;

    public MarketController(MarketService marketService,
            com.tradeflow.market_service.service.UpStoxAuthService upStoxAuthService) {
        this.marketService = marketService;
        this.upStoxAuthService = upStoxAuthService;
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

    @GetMapping("/history/{symbol}")
    public List<PriceHistoryPoint> getHistory(
            @PathVariable String symbol,
            @RequestParam(name = "range", defaultValue = "TODAY") String range) {
        return marketService.getPriceHistory(symbol, range);
    }

    @GetMapping("/login")
    public String getUpstoxLoginUrl() {
        return upStoxAuthService.getLoginUrl();
    }

    @GetMapping("/upstox/callback")
    public String handleUpstoxCallback(@RequestParam("code") String code) {
        String token = upStoxAuthService.getAccessToken(code);
        return token != null ? "Token successfully generated: " + token : "Failed to generate Upstox token.";
    }
}
