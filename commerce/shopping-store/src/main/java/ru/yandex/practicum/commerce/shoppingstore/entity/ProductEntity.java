package ru.yandex.practicum.commerce.shoppingstore.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.commerce.api.dto.ProductCategory;
import ru.yandex.practicum.commerce.api.dto.ProductState;
import ru.yandex.practicum.commerce.api.dto.QuantityState;

import java.util.UUID;

@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID productId;
    private String productName;
    private String description;
    private String imageSrc;
    @Enumerated(EnumType.STRING)
    private QuantityState quantityState;
    @Enumerated(EnumType.STRING)
    private ProductState productState;
    @Enumerated(EnumType.STRING)
    private ProductCategory productCategory;
    private Double price;
}