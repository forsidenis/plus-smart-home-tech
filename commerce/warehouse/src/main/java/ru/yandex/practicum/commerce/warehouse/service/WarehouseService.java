package ru.yandex.practicum.commerce.warehouse.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.api.dto.*;
import ru.yandex.practicum.commerce.warehouse.entity.WarehouseProductEntity;
import ru.yandex.practicum.commerce.warehouse.repository.WarehouseRepository;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseService {
    private final WarehouseRepository warehouseRepository;

    private static final String[] ADDRESSES = new String[]{"ADDRESS_1", "ADDRESS_2"};
    private static final String CURRENT_ADDRESS = ADDRESSES[new Random(new SecureRandom().nextLong()).nextInt(ADDRESSES.length)];
    private static final String COUNTRY = "Russia";
    private static final String CITY = "Moscow";
    private static final String STREET = "Tverskaya";
    private static final String HOUSE = "1";
    private static final String FLAT = "1";

    @PostConstruct
    public void init() {
        log.info("Warehouse address: {}", CURRENT_ADDRESS);
    }

    @Transactional
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        if (warehouseRepository.existsById(request.getProductId())) {
            throw new RuntimeException("Product already exists in warehouse");
        }
        WarehouseProductEntity entity = WarehouseProductEntity.builder()
                .productId(request.getProductId())
                .quantity(0L)
                .fragile(request.getFragile())
                .width(request.getDimension().getWidth())
                .height(request.getDimension().getHeight())
                .depth(request.getDimension().getDepth())
                .weight(request.getWeight())
                .build();
        warehouseRepository.save(entity);
    }

    @Transactional
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        WarehouseProductEntity entity = warehouseRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found in warehouse"));
        entity.setQuantity(entity.getQuantity() + request.getQuantity());
        warehouseRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto cart) {
        log.info("Checking shopping cart: {}", cart);

        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean fragile = false;
        for (Map.Entry<UUID, Long> entry : cart.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Long requestedQty = entry.getValue();
            WarehouseProductEntity product = warehouseRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
            log.debug("Checking product {}: requested {}, available {}", productId, requestedQty, product.getQuantity());
            if (product.getQuantity() < requestedQty) {
                throw new RuntimeException("Not enough quantity for product " + productId);
            }
            totalWeight += product.getWeight() * requestedQty;
            if (product.getWidth() == null || product.getHeight() == null || product.getDepth() == null) {
                throw new RuntimeException("Product dimensions are incomplete for product " + productId);
            }
            double volume = product.getWidth() * product.getHeight() * product.getDepth();
            totalVolume += volume * requestedQty;
            if (product.getFragile()) fragile = true;
        }
        return BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(fragile)
                .build();
    }

    public AddressDto getWarehouseAddress() {
        return AddressDto.builder()
                .country(CURRENT_ADDRESS)
                .city(CURRENT_ADDRESS)
                .street(CURRENT_ADDRESS)
                .house(CURRENT_ADDRESS)
                .flat(CURRENT_ADDRESS)
                .build();
    }
}