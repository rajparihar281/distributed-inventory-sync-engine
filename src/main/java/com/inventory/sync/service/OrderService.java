package com.inventory.sync.service;

import com.inventory.sync.domain.*;
import com.inventory.sync.dto.CreateOrderRequest;
import com.inventory.sync.dto.OrderResponse;
import com.inventory.sync.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryService inventoryService;

    @Transactional
    public OrderResponse createAndReserveOrder(CreateOrderRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found with ID: " + request.getWarehouseId()));

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order(orderNumber, request.getChannel());

        // 1. Assemble Line Items & calculate totals
        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + itemReq.getProductId()));
            order.addItem(product, itemReq.getQuantity(), product.getPrice());
        }

        // 2. Persist initial order header
        Order savedOrder = orderRepository.save(order);

        // 3. Execute concurrency-safe reservations per line item
        for (OrderItem item : savedOrder.getItems()) {
            inventoryService.reserveStockWithLock(
                    item.getProduct().getId(),
                    warehouse.getId(),
                    item.getQuantity(),
                    savedOrder.getOrderNumber()
            );

            // Record reservation link
            InventoryReservation reservation = new InventoryReservation(item, warehouse, item.getQuantity());
            reservationRepository.save(reservation);
        }

        // 4. Transition Order State: CREATED -> RESERVED
        savedOrder.transitionTo(OrderStatus.RESERVED);
        savedOrder = orderRepository.save(savedOrder);

        return mapToResponse(savedOrder);
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .channel(order.getChannel())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream()
                        .map(item -> OrderResponse.OrderItemDto.builder()
                                .productId(item.getProduct().getId())
                                .productSku(item.getProduct().getSku())
                                .productName(item.getProduct().getName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build())
                        .toList())
                .build();
    }
}