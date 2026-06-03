package ru.yandex.practicum.commerce.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.api.client.OrderClient;
import ru.yandex.practicum.commerce.api.client.WarehouseClient;
import ru.yandex.practicum.commerce.api.dto.*;
import ru.yandex.practicum.commerce.delivery.entity.AddressEntity;
import ru.yandex.practicum.commerce.delivery.entity.DeliveryEntity;
import ru.yandex.practicum.commerce.delivery.repository.DeliveryRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final WarehouseClient warehouseClient;
    private final OrderClient orderClient;

    private static final double BASE_COST = 5.0;
    private static final String WAREHOUSE_ADDRESS_1 = "ADDRESS_1";
    private static final String WAREHOUSE_ADDRESS_2 = "ADDRESS_2";

    /**
     * Расчёт стоимости доставки согласно алгоритму ТЗ.
     */
    public double calculateDeliveryCost(OrderDto order) {
        // Получаем адрес склада из Warehouse
        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress();
        AddressDto customerAddress = order.getDeliveryAddress();
        if (customerAddress == null) {
            throw new RuntimeException("Delivery address is missing in order");
        }

        double cost = BASE_COST;

        // 1. Коэффициент склада
        String warehouseStreet = warehouseAddress.getStreet();
        if (WAREHOUSE_ADDRESS_1.equals(warehouseStreet)) {
            cost = cost * 1 + BASE_COST;
        } else if (WAREHOUSE_ADDRESS_2.equals(warehouseStreet)) {
            cost = cost * 2 + BASE_COST;
        } else {
            cost = cost * 1 + BASE_COST; // по умолчанию
        }

        // 2. Хрупкость
        if (order.getFragile() != null && order.getFragile()) {
            cost += cost * 0.2;
        }

        // 3. Вес
        Double weight = order.getDeliveryWeight();
        if (weight != null) {
            cost += weight * 0.3;
        }

        // 4. Объём
        Double volume = order.getDeliveryVolume();
        if (volume != null) {
            cost += volume * 0.2;
        }

        // 5. Совпадение улицы
        if (!customerAddress.getStreet().equals(warehouseStreet)) {
            cost += cost * 0.2;
        }

        return cost;
    }

    /**
     * Создание новой доставки (планирование).
     */
    @Transactional
    public DeliveryDto planDelivery(DeliveryDto deliveryDto) {
        // Конвертируем AddressDto в AddressEntity
        AddressEntity fromAddress = toAddressEntity(deliveryDto.getFromAddress());
        AddressEntity toAddress = toAddressEntity(deliveryDto.getToAddress());

        DeliveryEntity entity = DeliveryEntity.builder()
                .deliveryId(UUID.randomUUID())
                .orderId(deliveryDto.getOrderId())
                .fromAddress(fromAddress)
                .toAddress(toAddress)
                .deliveryState(DeliveryState.CREATED)
                .build();
        entity = deliveryRepository.save(entity);

        return toDeliveryDto(entity);
    }

    /**
     * Приём товаров в доставку (статус IN_PROGRESS).
     * Вызывается, когда служба доставки получила заказ-наряд.
     */
    @Transactional
    public void pickDelivery(UUID orderId) {
        DeliveryEntity delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order: " + orderId));

        if (delivery.getDeliveryState() != DeliveryState.CREATED) {
            throw new RuntimeException("Delivery is not in CREATED state");
        }

        delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
        deliveryRepository.save(delivery);

        // Уведомляем Warehouse, что товары переданы в доставку
        ShippedToDeliveryRequest request = ShippedToDeliveryRequest.builder()
                .orderId(orderId)
                .deliveryId(delivery.getDeliveryId())
                .build();
        warehouseClient.shippedToDelivery(request);
    }

    /**
     * Успешная доставка.
     */
    @Transactional
    public void successfulDelivery(UUID orderId) {
        DeliveryEntity delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order: " + orderId));

        if (delivery.getDeliveryState() != DeliveryState.IN_PROGRESS) {
            throw new RuntimeException("Delivery is not in IN_PROGRESS state");
        }

        delivery.setDeliveryState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);

        // Уведомляем Order об успешной доставке
        orderClient.delivery(orderId, delivery.getDeliveryId());
    }

    /**
     * Неудачная доставка.
     */
    @Transactional
    public void failedDelivery(UUID orderId) {
        DeliveryEntity delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order: " + orderId));

        delivery.setDeliveryState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);

        // Уведомляем Order об ошибке доставки
        orderClient.deliveryFailed(orderId);
    }

    // Вспомогательные методы конвертации
    private AddressEntity toAddressEntity(AddressDto dto) {
        if (dto == null) return null;
        return AddressEntity.builder()
                .country(dto.getCountry())
                .city(dto.getCity())
                .street(dto.getStreet())
                .house(dto.getHouse())
                .flat(dto.getFlat())
                .build();
    }

    private AddressDto toAddressDto(AddressEntity entity) {
        if (entity == null) return null;
        return AddressDto.builder()
                .country(entity.getCountry())
                .city(entity.getCity())
                .street(entity.getStreet())
                .house(entity.getHouse())
                .flat(entity.getFlat())
                .build();
    }

    private DeliveryDto toDeliveryDto(DeliveryEntity entity) {
        return DeliveryDto.builder()
                .deliveryId(entity.getDeliveryId())
                .orderId(entity.getOrderId())
                .fromAddress(toAddressDto(entity.getFromAddress()))
                .toAddress(toAddressDto(entity.getToAddress()))
                .deliveryState(entity.getDeliveryState())
                .build();
    }
}