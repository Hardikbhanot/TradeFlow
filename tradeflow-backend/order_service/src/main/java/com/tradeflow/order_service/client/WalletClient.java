package com.tradeflow.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.math.BigDecimal;

@FeignClient(name = "wallet-service")
public interface WalletClient {
    @GetMapping("/api/v1/wallets/user/{userId}/balance")
    BigDecimal getBalance(@PathVariable("userId") Long userId);
}
