package ru.yandex.practicum.commerce.shoppingstore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.api.dto.ProductCategory;
import ru.yandex.practicum.commerce.api.dto.ProductDto;
import ru.yandex.practicum.commerce.api.dto.SetProductQuantityStateRequest;
import ru.yandex.practicum.commerce.shoppingstore.service.ProductService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-store")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public Page<ProductDto> getProducts(@RequestParam ProductCategory category, Pageable pageable) {
        return productService.getProducts(category, pageable);
    }

    @GetMapping("/{productId}")
    public ProductDto getProduct(@PathVariable UUID productId) {
        return productService.getProduct(productId);
    }

    @PutMapping
    public ProductDto createNewProduct(@RequestBody ProductDto productDto) {
        return productService.createNewProduct(productDto);
    }

    @PostMapping
    public ProductDto updateProduct(@RequestBody ProductDto productDto) {
        return productService.updateProduct(productDto);
    }

    @PostMapping("/removeProductFromStore")
    public boolean removeProductFromStore(@RequestBody UUID productId) {
        return productService.removeProductFromStore(productId);
    }

    @PostMapping("/quantityState")
    public boolean setProductQuantityState(@RequestBody SetProductQuantityStateRequest request) {
        return productService.setProductQuantityState(request);
    }
}