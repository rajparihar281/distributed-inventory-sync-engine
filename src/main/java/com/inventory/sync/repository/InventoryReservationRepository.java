package com.inventory.sync.repository;

import com.inventory.sync.domain.InventoryReservation;
import com.inventory.sync.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    List<InventoryReservation> findByOrderItemId(Long orderItemId);
    List<InventoryReservation> findByOrderItemIdAndStatus(Long orderItemId, ReservationStatus status);
}