package com.tradeflow.order_service.controller;

import com.tradeflow.order_service.dto.OrderRequest;
import com.tradeflow.order_service.entity.Order;
import com.tradeflow.order_service.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> placeOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody OrderRequest request) {

        request.setUserId(userId);
        Order newOrder = orderService.placeOrder(request);
        return ResponseEntity.ok(newOrder);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }
}