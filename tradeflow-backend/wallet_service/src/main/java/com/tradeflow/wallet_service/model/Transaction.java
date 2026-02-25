package com.tradeflow.wallet_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long walletId;
    private BigDecimal amount;
    
    // CREDIT (Adding money) or DEBIT (Spending money)
    private String type; 

    private String status; // SUCCESS, PENDING, FAILED
    
    private String referenceId; // External ID to prevent duplicates
    
    private LocalDateTime timestamp;
}