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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final WarehouseClient warehouseClient;
    private final OrderClient orderClient;

    // Константы для расчёта стоимости доставки
    private static final BigDecimal BASE_COST = BigDecimal.valueOf(5.0);
    private static final String WAREHOUSE_ADDRESS_1 = "ADDRESS_1";
    private static final String WAREHOUSE_ADDRESS_2 = "ADDRESS_2";
    private static final BigDecimal ADDRESS_FACTOR_1 = BigDecimal.ONE;
    private static final BigDecimal ADDRESS_FACTOR_2 = BigDecimal.valueOf(2);
    private static final BigDecimal FRAGILE_FACTOR = BigDecimal.valueOf(0.2);
    private static final BigDecimal WEIGHT_FACTOR = BigDecimal.valueOf(0.3);
    private static final BigDecimal VOLUME_FACTOR = BigDecimal.valueOf(0.2);
    private static final BigDecimal STREET_MISMATCH_FACTOR = BigDecimal.valueOf(0.2);

    /**
     * Расчёт стоимости доставки.
     * Логирование каждого этапа для отладки.
     */
    public BigDecimal calculateDeliveryCost(OrderDto order) {
        log.info("Начало расчёта стоимости доставки для заказа: {}", order.getOrderId());

        AddressDto warehouseAddr = warehouseClient.getWarehouseAddress();
        AddressDto customerAddr = order.getDeliveryAddress();

        if (customerAddr == null) {
            log.error("Адрес доставки отсутствует для заказа: {}", order.getOrderId());
            throw new RuntimeException("Delivery address is missing in order");
        }

        log.debug("Адрес склада: {}", warehouseAddr);
        log.debug("Адрес клиента: {}", customerAddr);

        BigDecimal cost = BASE_COST;
        log.info("Базовая стоимость: {}", cost);

        // 1. Учёт адреса склада
        String warehouseStreet = warehouseAddr.getStreet();
        if (WAREHOUSE_ADDRESS_1.equals(warehouseStreet)) {
            cost = cost.multiply(ADDRESS_FACTOR_1).add(BASE_COST);
            log.debug("Коэффициент склада ADDRESS_1: новое значение = {}", cost);
        } else if (WAREHOUSE_ADDRESS_2.equals(warehouseStreet)) {
            cost = cost.multiply(ADDRESS_FACTOR_2).add(BASE_COST);
            log.debug("Коэффициент склада ADDRESS_2: новое значение = {}", cost);
        } else {
            cost = cost.multiply(ADDRESS_FACTOR_1).add(BASE_COST);
            log.debug("Склад по умолчанию: новое значение = {}", cost);
        }

        // 2. Хрупкость
        if (Boolean.TRUE.equals(order.getFragile())) {
            BigDecimal fragileAddition = cost.multiply(FRAGILE_FACTOR);
            cost = cost.add(fragileAddition);
            log.debug("Добавка за хрупкость: {}, итого = {}", fragileAddition, cost);
        }

        // 3. Вес
        double weight = order.getDeliveryWeight() != null ? order.getDeliveryWeight() : 0.0;
        BigDecimal weightAddition = BigDecimal.valueOf(weight).multiply(WEIGHT_FACTOR);
        cost = cost.add(weightAddition);
        log.debug("Добавка за вес ({}) = {}, итого = {}", weight, weightAddition, cost);

        // 4. Объём
        double volume = order.getDeliveryVolume() != null ? order.getDeliveryVolume() : 0.0;
        BigDecimal volumeAddition = BigDecimal.valueOf(volume).multiply(VOLUME_FACTOR);
        cost = cost.add(volumeAddition);
        log.debug("Добавка за объём ({}) = {}, итого = {}", volume, volumeAddition, cost);

        // 5. Совпадение улицы
        if (!customerAddr.getStreet().equals(warehouseAddr.getStreet())) {
            BigDecimal mismatchAddition = cost.multiply(STREET_MISMATCH_FACTOR);
            cost = cost.add(mismatchAddition);
            log.debug("Добавка за несовпадение улицы: {}, итого = {}", mismatchAddition, cost);
        } else {
            log.debug("Улицы совпадают, добавки нет");
        }

        // Округление до 2 знаков (стандарт для денег)
        cost = cost.setScale(2, RoundingMode.HALF_UP);
        log.info("Финальная стоимость доставки для заказа {} = {}", order.getOrderId(), cost);
        return cost;
    }

    /**
     * Создание новой доставки.
     */
    @Transactional
    public DeliveryDto planDelivery(DeliveryDto deliveryDto) {
        log.info("Планирование доставки для заказа: {}", deliveryDto.getOrderId());

        AddressEntity from = AddressEntity.builder()
                .country(deliveryDto.getFromAddress().getCountry())
                .city(deliveryDto.getFromAddress().getCity())
                .street(deliveryDto.getFromAddress().getStreet())
                .house(deliveryDto.getFromAddress().getHouse())
                .flat(deliveryDto.getFromAddress().getFlat())
                .build();

        AddressEntity to = AddressEntity.builder()
                .country(deliveryDto.getToAddress().getCountry())
                .city(deliveryDto.getToAddress().getCity())
                .street(deliveryDto.getToAddress().getStreet())
                .house(deliveryDto.getToAddress().getHouse())
                .flat(deliveryDto.getToAddress().getFlat())
                .build();

        DeliveryEntity entity = DeliveryEntity.builder()
                .deliveryId(UUID.randomUUID())
                .orderId(deliveryDto.getOrderId())
                .fromAddress(from)
                .toAddress(to)
                .deliveryState(DeliveryState.CREATED)
                .build();

        entity = deliveryRepository.save(entity);
        log.info("Доставка создана с ID: {} для заказа: {}", entity.getDeliveryId(), entity.getOrderId());

        return DeliveryDto.builder()
                .deliveryId(entity.getDeliveryId())
                .orderId(entity.getOrderId())
                .fromAddress(deliveryDto.getFromAddress())
                .toAddress(deliveryDto.getToAddress())
                .deliveryState(entity.getDeliveryState())
                .build();
    }

    /**
     * Эмуляция получения товара в доставку.
     */
    @Transactional
    public void pickDelivery(UUID orderId) {
        log.info("Отметка о получении товара в доставку для заказа: {}", orderId);

        DeliveryEntity delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Доставка не найдена для заказа: {}", orderId);
                    return new RuntimeException("Delivery not found for order " + orderId);
                });

        if (delivery.getDeliveryState() != DeliveryState.CREATED) {
            log.warn("Доставка {} уже в процессе или завершена, текущий статус: {}", delivery.getDeliveryId(), delivery.getDeliveryState());
            throw new RuntimeException("Delivery already in progress or finished");
        }

        delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
        deliveryRepository.save(delivery);

        // Уведомляем склад
        ShippedToDeliveryRequest request = ShippedToDeliveryRequest.builder()
                .orderId(orderId)
                .deliveryId(delivery.getDeliveryId())
                .build();
        warehouseClient.shippedToDelivery(request);

        log.info("Доставка {} переведена в статус IN_PROGRESS для заказа {}", delivery.getDeliveryId(), orderId);
    }

    /**
     * Успешная доставка.
     */
    @Transactional
    public void successfulDelivery(UUID orderId) {
        log.info("Успешная доставка заказа: {}", orderId);

        DeliveryEntity delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Доставка не найдена для заказа: {}", orderId);
                    return new RuntimeException("Delivery not found for order " + orderId);
                });

        delivery.setDeliveryState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);

        orderClient.delivery(orderId, delivery.getDeliveryId());

        log.info("Доставка {} завершена успешно для заказа {}", delivery.getDeliveryId(), orderId);
    }

    /**
     * Ошибка доставки.
     */
    @Transactional
    public void failedDelivery(UUID orderId) {
        log.error("Ошибка доставки для заказа: {}", orderId);

        DeliveryEntity delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Доставка не найдена для заказа: {}", orderId);
                    return new RuntimeException("Delivery not found for order " + orderId);
                });

        delivery.setDeliveryState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);

        orderClient.deliveryFailed(orderId);

        log.info("Доставка {} отмечена как FAILED для заказа {}", delivery.getDeliveryId(), orderId);
    }
}