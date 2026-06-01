package com.tradeflow.market_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IndMoneyAuthService {

    @Value("${indmoney.api.secret:}")
    private String apiSecret;

    public String getStaticToken() {
        return apiSecret;
    }
}
