package com.tradeflow.wallet_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "wallet_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private BigDecimal amount;

    // Values: DEPOSIT, WITHDRAWAL, TRADE_BUY, TRADE_SELL
    private String type;

    // Values: PENDING, SUCCESS, FAILED
    private String status;

    // A unique ID from the "Payment Gateway" (or our Mock)
    @Column(unique = true)
    private String referenceId;

    private LocalDateTime timestamp;
}
