package com.tradeflow.order_service.dto;

import lombok.Data;

@Data
public class FundsReservedEvent {
    private Long orderId;
    private Long userId;
}