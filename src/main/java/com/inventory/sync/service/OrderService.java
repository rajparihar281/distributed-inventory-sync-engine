package com.inventory.sync.service;

import com.inventory.sync.domain.*;
import com.inventory.sync.dto.CreateOrderRequest;
import com.inventory.sync.dto.OrderResponse;
import com.inventory.sync.repository.*;
import com.inventory.sync.strategy.FulfillmentStrategy;
import com.inventory.sync.strategy.FulfillmentStrategyFactory;
import com.inventory.sync.strategy.WarehouseAllocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryService inventoryService;
    private final FulfillmentStrategyFactory strategyFactory;

    @Transactional
    public OrderResponse createAndReserveOrder(CreateOrderRequest request) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order(orderNumber, request.getChannel());

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + itemReq.getProductId()));
            order.addItem(product, itemReq.getQuantity(), product.getPrice());
        }

        Order savedOrder = orderRepository.save(order);

        if (request.getWarehouseId() != null) {
            Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                    .orElseThrow(() -> new IllegalArgumentException("Warehouse not found with ID: " + request.getWarehouseId()));

            for (OrderItem item : savedOrder.getItems()) {
                inventoryService.reserveStockWithLock(
                        item.getProduct().getId(),
                        warehouse.getId(),
                        item.getQuantity(),
                        savedOrder.getOrderNumber()
                );
                reservationRepository.save(new InventoryReservation(item, warehouse, item.getQuantity()));
            }
        } else {
            FulfillmentStrategy strategy = strategyFactory.getStrategy(request.getFulfillmentStrategy());

            for (OrderItem item : savedOrder.getItems()) {
                List<WarehouseAllocation> allocations = strategy.allocateStock(item.getProduct(), item.getQuantity());

                for (WarehouseAllocation allocation : allocations) {
                    inventoryService.reserveStockWithLock(
                            item.getProduct().getId(),
                            allocation.getWarehouse().getId(),
                            allocation.getAllocatedQuantity(),
                            savedOrder.getOrderNumber()
                    );
                    reservationRepository.save(new InventoryReservation(item, allocation.getWarehouse(), allocation.getAllocatedQuantity()));
                }
            }
        }

        savedOrder.transitionTo(OrderStatus.RESERVED);
        savedOrder = orderRepository.save(savedOrder);

        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        order.transitionTo(OrderStatus.CANCELLED);

        for (OrderItem item : order.getItems()) {
            List<InventoryReservation> activeReservations = reservationRepository
                    .findByOrderItemIdAndStatus(item.getId(), ReservationStatus.ACTIVE);

            for (InventoryReservation res : activeReservations) {
                inventoryService.releaseReservation(
                        item.getProduct().getId(),
                        res.getWarehouse().getId(),
                        res.getReservedQty(),
                        order.getOrderNumber()
                );
                res.setStatus(ReservationStatus.RELEASED);
                reservationRepository.save(res);
            }
        }

        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse dispatchOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        if (order.getStatus() == OrderStatus.RESERVED) {
            order.transitionTo(OrderStatus.CONFIRMED);
            order.transitionTo(OrderStatus.PACKING);
            order.transitionTo(OrderStatus.DISPATCHED);
        } else {
            throw new IllegalStateException("Order must be in RESERVED state to initiate dispatch");
        }

        for (OrderItem item : order.getItems()) {
            List<InventoryReservation> activeReservations = reservationRepository
                    .findByOrderItemIdAndStatus(item.getId(), ReservationStatus.ACTIVE);

            for (InventoryReservation res : activeReservations) {
                inventoryService.dispatchStock(
                        item.getProduct().getId(),
                        res.getWarehouse().getId(),
                        res.getReservedQty(),
                        order.getOrderNumber()
                );

                res.setStatus(ReservationStatus.COMMITTED);
                reservationRepository.save(res);
            }
        }

        Order savedOrder = orderRepository.save(order);
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