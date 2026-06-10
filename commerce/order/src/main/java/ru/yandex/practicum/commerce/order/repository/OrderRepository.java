package ru.yandex.practicum.commerce.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.commerce.order.entity.OrderEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findByUsername(String username);
    Optional<OrderEntity> findByOrderIdAndUsername(UUID orderId, String username);
}