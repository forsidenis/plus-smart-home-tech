package ru.yandex.practicum.commerce.shoppingstore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.commerce.api.dto.ProductCategory;
import ru.yandex.practicum.commerce.api.dto.ProductState;
import ru.yandex.practicum.commerce.shoppingstore.entity.ProductEntity;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {
    Page<ProductEntity> findByProductCategoryAndProductState(ProductCategory category, ProductState state, Pageable pageable);
    Optional<ProductEntity> findByProductIdAndProductState(UUID productId, ProductState state);
    Page<ProductEntity> findByProductCategory(ProductCategory category, Pageable pageable);
}