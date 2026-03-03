package com.tradeflow.portfolio_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "holdings")
@Data
@NoArgsConstructor // 👈 Hibernate needs this!
@AllArgsConstructor
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String symbol;
    private String exchange;
    private Integer totalQuantity;

    // 🛡️ Precision 10, Scale 2 is standard for INR (e.g., 2500.50)
    @Column(precision = 10, scale = 2) 
    private BigDecimal averageBuyPrice;
}