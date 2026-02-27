package com.tradeflow.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "wallet-service") 
public interface WalletClient {

    @PostMapping("/api/v1/wallets/create")
    void createWallet(@RequestBody Map<String, Object> walletRequest);
}