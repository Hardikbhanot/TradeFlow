package com.tradeflow.order_service.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  
@AllArgsConstructor
public class FundsReservedEvent {
    private Long orderId;
    private Long userId;
}