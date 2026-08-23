package com.inventory.sync.controller;

import com.inventory.sync.dto.CreateOrderRequest;
import com.inventory.sync.dto.OrderResponse;
import com.inventory.sync.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createAndReserveOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderNumber) {
        OrderResponse response = orderService.cancelOrder(orderNumber);
        return ResponseEntity.ok(response);
    }
}