package com.tradeflow.order_service.dto;

import lombok.Data;

@Data
public class FundsRejectedEvent {
    private Long orderId;
    private Long userId;
    private String reason;
}