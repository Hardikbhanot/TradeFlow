package com.tradeflow.portfolio_service.controller;

import com.tradeflow.portfolio_service.entity.Holding;
import com.tradeflow.portfolio_service.service.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Holding>> getDashboard(@PathVariable Long userId) {
        return ResponseEntity.ok(portfolioService.getUserHoldings(userId));
    }

    @GetMapping("/check-holdings")
    public ResponseEntity<Boolean> checkHoldings(
            @RequestParam("userId") Long userId,
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "exchange", required = false) String exchange,
            @RequestParam("quantity") Integer quantity) {
        return ResponseEntity.ok(portfolioService.hasEnoughShares(userId, symbol, exchange, quantity));
    }
}