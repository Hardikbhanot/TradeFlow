package com.tradeflow.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.math.BigDecimal;

@FeignClient(name = "market-service")
public interface MarketClient {
    @GetMapping("/api/v1/market/price/{symbol}")
    BigDecimal getLivePrice(@PathVariable("symbol") String symbol);
}