package com.tradeflow.wallet_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Published to "ledger-transaction-topic" after every ledger entry.
 * The notification-service consumes this to send wallet confirmation emails.
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
