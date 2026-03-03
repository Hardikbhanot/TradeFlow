package com.tradeflow.market_service.controller;

import com.tradeflow.market_service.service.MarketService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping("/price/{symbol}")
    public BigDecimal getPrice(@PathVariable String symbol) {
        return marketService.getLivePrice(symbol);
    }
}