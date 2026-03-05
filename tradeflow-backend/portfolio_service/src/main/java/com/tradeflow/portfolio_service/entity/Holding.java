package com.tradeflow.portfolio_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "holdings")
@Data
@NoArgsConstructor 
@AllArgsConstructor
public class Holding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String symbol;
    private String exchange;
    private Integer totalQuantity;
    private BigDecimal avgPrice;   
}