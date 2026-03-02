package com.tradeflow.wallet_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FundsReservedEvent {
    private Long orderId;
    private Long userId;
}