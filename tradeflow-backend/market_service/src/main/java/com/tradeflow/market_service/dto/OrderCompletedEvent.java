package com.tradeflow.market_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCompletedEvent {
    private Long orderId;
    private Long userId;
    private String symbol;
    private String exchange;
    private BigDecimal quantity; 
    private BigDecimal price;
    private String side; 
}
