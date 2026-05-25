package ru.yandex.practicum.commerce.shoppingcart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.api.client.WarehouseClient;
import ru.yandex.practicum.commerce.api.dto.ShoppingCartDto;
import ru.yandex.practicum.commerce.shoppingcart.entity.CartEntity;
import ru.yandex.practicum.commerce.shoppingcart.repository.CartRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final CartRepository cartRepository;
    private final WarehouseClient warehouseClient;

    @Transactional
    public ShoppingCartDto getShoppingCart(String username) {
        CartEntity cart = getOrCreateCart(username);
        return toDto(cart);
    }

    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Long> productsToAdd) {
        CartEntity cart = getOrCreateCart(username);
        if (!cart.isActive()) {
            throw new RuntimeException("Cart is deactivated");
        }
        // обновляем корзину
        Map<UUID, Long> currentProducts = cart.getProducts();
        if (currentProducts == null) {
            currentProducts = new HashMap<>();
            cart.setProducts(currentProducts);
        }
        for (Map.Entry<UUID, Long> entry : productsToAdd.entrySet()) {
            currentProducts.merge(entry.getKey(), entry.getValue(), Long::sum);
        }
        // проверяем наличие на складе
        ShoppingCartDto dto = toDto(cart);
        log.info("Cart DTO before warehouse check: {}", dto);
        try {
            warehouseClient.checkProductQuantityEnoughForShoppingCart(dto);
        } catch (Exception e) {
            log.error("Warehouse check failed", e);
            throw new RuntimeException("Not enough products in warehouse", e);
        }
        cartRepository.save(cart);
        return dto;
    }

    @Transactional
    public ShoppingCartDto removeFromShoppingCart(String username, java.util.List<UUID> productIds) {
        CartEntity cart = getOrCreateCart(username);
        if (!cart.isActive()) {
            throw new RuntimeException("Cart is deactivated");
        }
        Map<UUID, Long> products = cart.getProducts();
        if (products == null) {
            throw new RuntimeException("No products in cart");
        }
        for (UUID id : productIds) {
            products.remove(id);
        }
        cartRepository.save(cart);
        return toDto(cart);
    }

    @Transactional
    public ShoppingCartDto changeProductQuantity(String username, UUID productId, Long newQuantity) {
        CartEntity cart = getOrCreateCart(username);
        if (!cart.isActive()) {
            throw new RuntimeException("Cart is deactivated");
        }
        Map<UUID, Long> products = cart.getProducts();
        if (products == null || !products.containsKey(productId)) {
            throw new RuntimeException("Product not in cart");
        }
        products.put(productId, newQuantity);

        ShoppingCartDto dto = toDto(cart);
        log.info("Cart DTO before warehouse check: {}", dto);
        try {
            warehouseClient.checkProductQuantityEnoughForShoppingCart(dto);
        } catch (Exception e) {
            log.error("Warehouse check failed", e);
            throw new RuntimeException("Not enough products in warehouse", e);
        }
        cartRepository.save(cart);
        return dto;
    }

    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        CartEntity cart = cartRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new RuntimeException("Active cart not found"));
        cart.setActive(false);
        cartRepository.save(cart);
    }

    private CartEntity getOrCreateCart(String username) {
        return cartRepository.findByUsernameAndActiveTrue(username)
                .orElseGet(() -> {
                    CartEntity newCart = CartEntity.builder()
                            .username(username)
                            .active(true)
                            .products(new HashMap<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private ShoppingCartDto toDto(CartEntity entity) {
        return ShoppingCartDto.builder()
                .shoppingCartId(entity.getShoppingCartId())
                .products(entity.getProducts())
                .build();
    }
}