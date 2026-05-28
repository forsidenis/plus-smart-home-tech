package ru.yandex.practicum.commerce.warehouse.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "warehouse_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseProductEntity {
    @Id
    private UUID productId;
    private Long quantity;
    private Boolean fragile;
    private Double width;
    private Double height;
    private Double depth;
    private Double weight;
}