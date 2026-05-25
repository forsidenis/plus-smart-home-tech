package ru.yandex.practicum.commerce.shoppingstore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.api.dto.*;
import ru.yandex.practicum.commerce.shoppingstore.entity.ProductEntity;
import ru.yandex.practicum.commerce.shoppingstore.repository.ProductRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(ProductCategory category, Pageable pageable) {
        return productRepository.findByProductCategoryAndProductState(category, ProductState.ACTIVE, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ProductDto getProduct(UUID productId) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return toDto(entity);
    }

    @Transactional
    public ProductDto createNewProduct(ProductDto productDto) {
        ProductEntity entity = ProductEntity.builder()
                .productId(productDto.getProductId())
                .productName(productDto.getProductName())
                .description(productDto.getDescription())
                .imageSrc(productDto.getImageSrc())
                .quantityState(productDto.getQuantityState() != null ? productDto.getQuantityState() : QuantityState.ENOUGH)
                .productState(ProductState.ACTIVE)
                .productCategory(productDto.getProductCategory())
                .price(productDto.getPrice())
                .build();
        entity = productRepository.save(entity);
        return toDto(entity);
    }

    @Transactional
    public ProductDto updateProduct(ProductDto productDto) {
        ProductEntity entity = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        entity.setProductName(productDto.getProductName());
        entity.setDescription(productDto.getDescription());
        entity.setImageSrc(productDto.getImageSrc());
        entity.setQuantityState(productDto.getQuantityState());
        entity.setProductCategory(productDto.getProductCategory());
        entity.setPrice(productDto.getPrice());
        return toDto(productRepository.save(entity));
    }

    @Transactional
    public boolean removeProductFromStore(UUID productId) {
        ProductEntity entity = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        entity.setProductState(ProductState.DEACTIVATE);
        productRepository.save(entity);
        return true;
    }

    @Transactional
    public boolean setProductQuantityState(SetProductQuantityStateRequest request) {
        ProductEntity entity = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        entity.setQuantityState(request.getQuantityState());
        productRepository.save(entity);
        return true;
    }

    private ProductDto toDto(ProductEntity entity) {
        return ProductDto.builder()
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .description(entity.getDescription())
                .imageSrc(entity.getImageSrc())
                .quantityState(entity.getQuantityState())
                .productState(entity.getProductState())
                .productCategory(entity.getProductCategory())
                .price(entity.getPrice())
                .build();
    }
}