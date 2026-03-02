package com.tradeflow.wallet_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FundsRejectedEvent {
    private Long orderId;
    private Long userId;
    private String reason; 
}