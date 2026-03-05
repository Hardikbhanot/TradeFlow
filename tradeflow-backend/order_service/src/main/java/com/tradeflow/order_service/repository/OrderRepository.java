package com.tradeflow.order_service.repository;

import com.tradeflow.order_service.entity.Order;
import com.tradeflow.order_service.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);
}