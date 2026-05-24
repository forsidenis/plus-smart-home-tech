package ru.yandex.practicum.commerce.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.commerce.warehouse.entity.WarehouseProductEntity;

import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<WarehouseProductEntity, UUID> {
}