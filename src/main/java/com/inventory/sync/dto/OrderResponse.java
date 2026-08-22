package com.inventory.sync.dto;

import com.inventory.sync.domain.OrderChannel;
import com.inventory.sync.domain.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private OrderChannel channel;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class OrderItemDto {
        private Long productId;
        private String productSku;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}