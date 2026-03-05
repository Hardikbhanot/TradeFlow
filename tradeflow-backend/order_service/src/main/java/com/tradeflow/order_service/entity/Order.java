package com.tradeflow.order_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.tradeflow.order_service.enums.OrderSide;
import com.tradeflow.order_service.enums.OrderStatus;
import com.tradeflow.order_service.enums.OrderType;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String symbol;
    private Integer quantity;
    private String exchange;

    @Enumerated(EnumType.STRING)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    private OrderType type; // (MARKET/LIMIT)

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // private BigDecimal pricePerUnit;

    private BigDecimal triggerPrice;
    private BigDecimal executedPrice;
}