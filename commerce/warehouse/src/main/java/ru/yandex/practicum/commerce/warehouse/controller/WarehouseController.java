package ru.yandex.practicum.commerce.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.api.dto.*;
import ru.yandex.practicum.commerce.warehouse.service.WarehouseService;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController {
    private final WarehouseService warehouseService;

    @PutMapping
    public void newProductInWarehouse(@RequestBody NewProductInWarehouseRequest request) {
        warehouseService.newProductInWarehouse(request);
    }

    @PostMapping("/check")
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(@RequestBody ShoppingCartDto shoppingCartDto) {
        return warehouseService.checkProductQuantityEnoughForShoppingCart(shoppingCartDto);
    }

    @PostMapping("/add")
    public void addProductToWarehouse(@RequestBody AddProductToWarehouseRequest request) {
        warehouseService.addProductToWarehouse(request);
    }

    @GetMapping("/address")
    public AddressDto getWarehouseAddress() {
        return warehouseService.getWarehouseAddress();
    }

    @PostMapping("/assembly")
    public BookedProductsDto assemblyProductsForOrder(@RequestBody AssemblyProductsForOrderRequest request) {
        return warehouseService.assemblyProductsForOrder(request);
    }

    @PostMapping("/shipped")
    public void shippedToDelivery(@RequestBody ShippedToDeliveryRequest request) {
        warehouseService.shippedToDelivery(request);
    }

    @PostMapping("/return")
    public void acceptReturn(@RequestBody Map<UUID, Long> products) {
        warehouseService.acceptReturn(products);
    }
}