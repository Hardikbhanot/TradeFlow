package com.tradeflow.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal; 

@Data
@AllArgsConstructor 
@NoArgsConstructor  
public class WalletUpdateEvent {
    private Long userId;
    private BigDecimal amount;
    private String transactionType; 
}