package com.inventory.sync;

import com.inventory.sync.domain.Inventory;
import com.inventory.sync.domain.OrderChannel;
import com.inventory.sync.dto.CreateOrderRequest;
import com.inventory.sync.repository.InventoryRepository;
import com.inventory.sync.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ConcurrentOrderProcessingTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("Should prevent overselling when concurrent threads compete for limited inventory")
    void testConcurrentOrderReservations() throws InterruptedException {
        // Product 1 at Warehouse 1 currently has available_qty = 6
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Thread 1: Order 4 units
        executor.submit(() -> {
            try {
                startLatch.await(); // Wait for sync start signal
                CreateOrderRequest request = buildOrderRequest(1L, 1L, 4);
                orderService.createAndReserveOrder(request);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Order 3 units
        executor.submit(() -> {
            try {
                startLatch.await(); // Wait for sync start signal
                CreateOrderRequest request = buildOrderRequest(1L, 1L, 3);
                orderService.createAndReserveOrder(request);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        // Release the barrier: Both threads fire simultaneously
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Verification
        assertEquals(1, successCount.get(), "Exactly one concurrent order should succeed");
        assertEquals(1, failureCount.get(), "Exactly one concurrent order should fail due to insufficient stock");

        // Verify database invariants in MySQL
        Inventory finalInventory = inventoryRepository.findByProductIdAndWarehouseId(1L, 1L).orElseThrow();
        assertTrue(finalInventory.getAvailableQty() >= 0, "Available stock must never be negative");
        assertEquals(
                finalInventory.getPhysicalQty(),
                finalInventory.getAvailableQty() + finalInventory.getReservedQty() + finalInventory.getDamagedQty(),
                "Inventory invariant equation must hold"
        );
    }

    private CreateOrderRequest buildOrderRequest(Long productId, Long warehouseId, int quantity) {
        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest();
        itemReq.setProductId(productId);
        itemReq.setQuantity(quantity);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setChannel(OrderChannel.WEBSITE);
        req.setWarehouseId(warehouseId);
        req.setItems(List.of(itemReq));
        return req;
    }
}