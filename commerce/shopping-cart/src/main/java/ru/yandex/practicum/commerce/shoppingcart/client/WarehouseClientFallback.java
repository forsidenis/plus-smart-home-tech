package ru.yandex.practicum.commerce.shoppingcart.client;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.commerce.api.client.WarehouseClient;
import ru.yandex.practicum.commerce.api.dto.*;

import java.util.Map;
import java.util.UUID;

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

    @Override
    public BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request) {
        throw new RuntimeException("Сервис склада временно недоступен для сборки заказа");
    }

    @Override
    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        throw new RuntimeException("Сервис склада временно недоступен для передачи в доставку");
    }

    @Override
    public void acceptReturn(Map<UUID, Long> products) {
        throw new RuntimeException("Сервис склада временно недоступен для возврата товаров");
    }
}