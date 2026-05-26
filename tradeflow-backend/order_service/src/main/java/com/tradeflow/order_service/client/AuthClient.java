package com.tradeflow.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @PostMapping("/auth/otp/generate-for-sell")
    void generateOtpForSell(@RequestParam("userId") Long userId);

    @PostMapping("/auth/otp/verify-for-sell")
    boolean verifyOtpForSell(@RequestParam("userId") Long userId, @RequestParam("otp") String otp);
}
