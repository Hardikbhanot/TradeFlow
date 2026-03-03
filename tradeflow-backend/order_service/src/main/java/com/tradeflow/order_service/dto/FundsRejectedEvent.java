package com.tradeflow.order_service.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundsRejectedEvent {
    private Long orderId;
    private Long userId;
    private String reason;
}