package com.tradeflow.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Consumed from "ledger-transaction-topic" published by the wallet-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LedgerTransactionEvent {
    private Long userId;
    private BigDecimal amount;
    private String type; // DEPOSIT | WITHDRAWAL | TRADE_BUY | TRADE_SELL
    private String status; // SUCCESS | FAILED
    private String referenceId;
}
