package ru.yandex.practicum.commerce.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.commerce.warehouse.entity.OrderBookingEntity;

import java.util.Optional;
import java.util.UUID;

public interface OrderBookingRepository extends JpaRepository<OrderBookingEntity, UUID> {
    Optional<OrderBookingEntity> findByOrderId(UUID orderId);
}