package com.tradeflow.order_service.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderRequest {
    private Long userId; // For testing passing manually
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal pricePerUnit;
}