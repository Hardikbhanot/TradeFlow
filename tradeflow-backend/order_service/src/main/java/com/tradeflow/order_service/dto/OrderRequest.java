package com.tradeflow.order_service.dto;

import com.tradeflow.order_service.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor  
@AllArgsConstructor
public class OrderRequest {
    private Long userId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal pricePerUnit;
    private String exchange;
    private OrderType orderType;
}