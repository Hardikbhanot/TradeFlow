package com.tradeflow.market_service.controller;

import com.tradeflow.market_service.service.MarketService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

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