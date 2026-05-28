package ru.yandex.practicum.commerce.shoppingcart.client;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.commerce.api.client.WarehouseClient;
import ru.yandex.practicum.commerce.api.dto.*;

@Component
public class WarehouseClientFallback implements WarehouseClient {

    @Override
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        throw new RuntimeException("Сервис склада временно недоступен");
    }

    @Override
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto shoppingCartDto) {
        throw new RuntimeException("Сервис склада временно недоступен. Попробуйте позже.");
    }

    @Override
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        throw new RuntimeException("Сервис склада временно недоступен");
    }

    @Override
    public AddressDto getWarehouseAddress() {
        throw new RuntimeException("Сервис склада временно недоступен");
    }
}