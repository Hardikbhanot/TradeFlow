package com.tradeflow.wallet_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletUpdateEvent {
    private Long userId;
    private BigDecimal amount;
    private String transactionType;
    private String referenceId;
}
