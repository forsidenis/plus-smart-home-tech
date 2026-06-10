package ru.yandex.practicum.commerce.delivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.commerce.delivery.entity.DeliveryEntity;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<DeliveryEntity, UUID> {
    Optional<DeliveryEntity> findByOrderId(UUID orderId);
}