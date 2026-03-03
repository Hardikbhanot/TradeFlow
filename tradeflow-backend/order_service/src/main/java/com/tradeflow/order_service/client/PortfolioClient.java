package com.tradeflow.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "portfolio-service") 
public interface PortfolioClient {
    @GetMapping("/api/v1/portfolio/check-holdings")
    boolean hasEnoughShares(
            @RequestParam Long userId,
            @RequestParam String symbol,
            @RequestParam String exchange,
            @RequestParam Integer quantity);
}