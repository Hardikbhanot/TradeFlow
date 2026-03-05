package com.tradeflow.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import com.tradeflow.order_service.enums.OrderSide; 

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCompletedEvent {
    private Long orderId;
    private Long userId;
    private String symbol;
    private String exchange;
    private BigDecimal quantity; 
    private BigDecimal price;    // The ACTUAL execution price
    private OrderSide side;      // Changed from String "orderType"
}