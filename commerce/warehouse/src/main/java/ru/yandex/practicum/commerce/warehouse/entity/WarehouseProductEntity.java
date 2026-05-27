package ru.yandex.practicum.commerce.warehouse.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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