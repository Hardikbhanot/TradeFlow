package com.tradeflow.wallet_service.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private String orderType; // "BUY" or "SELL"
}