package com.tradeflow.notification_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service") // Uses Eureka to route the request dynamically
public interface AuthClient {

    @GetMapping("/auth/users/{id}/email")
    String getUserEmailById(@PathVariable("id") Long id);
}
