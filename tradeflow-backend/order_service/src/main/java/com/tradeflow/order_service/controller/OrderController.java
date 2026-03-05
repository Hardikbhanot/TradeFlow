package com.tradeflow.order_service.controller;

import com.tradeflow.order_service.dto.OrderRequest;
import com.tradeflow.order_service.entity.Order;
import com.tradeflow.order_service.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> placeOrder(@RequestBody OrderRequest request) {
        Order newOrder = orderService.placeOrder(request);

        return ResponseEntity.ok(newOrder);
    }

    // A simple GET endpoint to easily verify the service is running from a browser
    @GetMapping
    public ResponseEntity<String> getOrderServiceStatus() {
        return ResponseEntity
                .ok("Order Service is up and running! To place an order, send a POST request to this URL.");
    }
}