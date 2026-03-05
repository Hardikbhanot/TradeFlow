package com.tradeflow.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import com.tradeflow.order_service.enums.OrderSide;
import com.tradeflow.order_service.enums.OrderType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount; 
    
    private OrderSide side;      // Changed from String "orderType"
    private OrderType orderType; // Added to distinguish Market vs Limit
}