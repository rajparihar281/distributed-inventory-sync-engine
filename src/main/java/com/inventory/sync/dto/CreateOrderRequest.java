package com.inventory.sync.dto;

import com.inventory.sync.domain.OrderChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "Order channel is required")
    private OrderChannel channel;

    // Optional: If null, the fulfillment strategy dynamically allocates warehouses
    private Long warehouseId;

    // Optional: "singleWarehouseStrategy" or "splitShipmentStrategy" (defaults to split)
    private String fulfillmentStrategy;

    @NotEmpty(message = "Order must contain at least one line item")
    @Valid
    private List<OrderItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrderItemRequest {
        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotNull(message = "Quantity is required")
        private Integer quantity;
    }
}