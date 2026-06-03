package ru.yandex.practicum.commerce.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.api.client.*;
import ru.yandex.practicum.commerce.api.dto.*;
import ru.yandex.practicum.commerce.order.entity.OrderEntity;
import ru.yandex.practicum.commerce.order.repository.OrderRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final WarehouseClient warehouseClient;
    private final PaymentClient paymentClient;
    private final DeliveryClient deliveryClient;

    // Создание заказа
    @Transactional
    public OrderDto createNewOrder(CreateNewOrderRequest request, String username) {
        ShoppingCartDto cart = request.getShoppingCart();
        AddressDto deliveryAddress = request.getDeliveryAddress();

        // 1. Предварительная проверка наличия на складе
        BookedProductsDto booked = warehouseClient.checkProductQuantityEnoughForShoppingCart(cart);

        // 2. Создаём заказ со статусом NEW
        OrderEntity order = OrderEntity.builder()
                .orderId(UUID.randomUUID())
                .shoppingCartId(cart.getShoppingCartId())
                .products(cart.getProducts())
                .state(OrderState.NEW)
                .deliveryWeight(booked.getDeliveryWeight())
                .deliveryVolume(booked.getDeliveryVolume())
                .fragile(booked.getFragile())
                .deliveryAddress(deliveryAddress)
                .username(username)
                .build();
        order = orderRepository.save(order);

        // 3. Вызываем сборку товаров на складе (резервирование)
        AssemblyProductsForOrderRequest assemblyRequest = AssemblyProductsForOrderRequest.builder()
                .orderId(order.getOrderId())
                .products(order.getProducts())
                .build();
        warehouseClient.assemblyProductsForOrder(assemblyRequest);

        return toDto(order);
    }

    // Получение всех заказов пользователя
    public List<OrderDto> getClientOrders(String username) {
        return orderRepository.findByUsername(username).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Сборка заказа (успех)
    @Transactional
    public OrderDto assembly(UUID orderId) {
        OrderEntity order = getOrderOrThrow(orderId);
        if (order.getState() != OrderState.NEW) {
            throw new RuntimeException("Order cannot be assembled in state " + order.getState());
        }
        order.setState(OrderState.ASSEMBLED);
        return toDto(orderRepository.save(order));
    }

    // Ошибка сборки
    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        OrderEntity order = getOrderOrThrow(orderId);
        order.setState(OrderState.ASSEMBLY_FAILED);
        return toDto(orderRepository.save(order));
    }

    // Успешная оплата
    @Transactional
    public OrderDto payment(UUID orderId, UUID paymentId) {
        OrderEntity order = getOrderOrThrow(orderId);
        if (order.getState() != OrderState.ASSEMBLED) {
            throw new RuntimeException("Order must be ASSEMBLED before payment");
        }
        order.setState(OrderState.PAID);
        order.setPaymentId(paymentId);
        return toDto(orderRepository.save(order));
    }

    // Ошибка оплаты
    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        OrderEntity order = getOrderOrThrow(orderId);
        order.setState(OrderState.PAYMENT_FAILED);
        return toDto(orderRepository.save(order));
    }

    // Доставка успешна
    @Transactional
    public OrderDto delivery(UUID orderId, UUID deliveryId) {
        OrderEntity order = getOrderOrThrow(orderId);
        if (order.getState() != OrderState.PAID) {
            throw new RuntimeException("Order must be PAID before delivery");
        }
        order.setState(OrderState.DELIVERED);
        order.setDeliveryId(deliveryId);
        return toDto(orderRepository.save(order));
    }

    // Ошибка доставки
    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        OrderEntity order = getOrderOrThrow(orderId);
        order.setState(OrderState.DELIVERY_FAILED);
        return toDto(orderRepository.save(order));
    }

    // Завершение заказа (completed)
    @Transactional
    public OrderDto complete(UUID orderId) {
        OrderEntity order = getOrderOrThrow(orderId);
        order.setState(OrderState.COMPLETED);
        return toDto(orderRepository.save(order));
    }

    // Расчёт общей стоимости (без сохранения)
    public OrderDto calculateTotalCost(UUID orderId) {
        OrderEntity order = getOrderOrThrow(orderId);
        OrderDto orderDto = toDto(order);
        // получаем стоимость товаров
        Double productCost = paymentClient.productCost(orderDto);
        // получаем общую стоимость (товары + налог)
        Double totalWithTax = paymentClient.getTotalCost(orderDto);
        // получаем стоимость доставки
        Double deliveryCost = deliveryClient.deliveryCost(orderDto);

        orderDto.setProductPrice(productCost);
        orderDto.setDeliveryPrice(deliveryCost);
        orderDto.setTotalPrice(totalWithTax + deliveryCost); // общая с доставкой
        return orderDto;
    }

    // Расчёт стоимости доставки (без сохранения)
    public OrderDto calculateDeliveryCost(UUID orderId) {
        OrderEntity order = getOrderOrThrow(orderId);
        OrderDto orderDto = toDto(order);
        Double deliveryCost = deliveryClient.deliveryCost(orderDto);
        orderDto.setDeliveryPrice(deliveryCost);
        return orderDto;
    }

    // Возврат товаров
    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        OrderEntity order = getOrderOrThrow(request.getOrderId());
        if (order.getState() != OrderState.DELIVERED) {
            throw new RuntimeException("Can only return DELIVERED orders");
        }
        // вызываем склад для увеличения остатков
        warehouseClient.acceptReturn(request.getProducts());
        order.setState(OrderState.PRODUCT_RETURNED);
        return toDto(orderRepository.save(order));
    }

    // Деактивация/отмена заказа
    @Transactional
    public OrderDto cancel(UUID orderId) {
        OrderEntity order = getOrderOrThrow(orderId);
        if (order.getState() == OrderState.NEW || order.getState() == OrderState.ASSEMBLED) {
            order.setState(OrderState.CANCELED);
        } else {
            throw new RuntimeException("Cannot cancel order in state " + order.getState());
        }
        return toDto(orderRepository.save(order));
    }

    private OrderEntity getOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    private OrderDto toDto(OrderEntity entity) {
        return OrderDto.builder()
                .orderId(entity.getOrderId())
                .shoppingCartId(entity.getShoppingCartId())
                .products(entity.getProducts())
                .paymentId(entity.getPaymentId())
                .deliveryId(entity.getDeliveryId())
                .state(entity.getState())
                .deliveryWeight(entity.getDeliveryWeight())
                .deliveryVolume(entity.getDeliveryVolume())
                .fragile(entity.getFragile())
                .totalPrice(entity.getTotalPrice())
                .deliveryPrice(entity.getDeliveryPrice())
                .productPrice(entity.getProductPrice())
                .deliveryAddress(entity.getDeliveryAddress())
                .username(entity.getUsername())
                .build();
    }
}