package ru.yandex.practicum.commerce.delivery.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.commerce.api.dto.DeliveryDto;
import ru.yandex.practicum.commerce.api.dto.OrderDto;
import ru.yandex.practicum.commerce.delivery.service.DeliveryService;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryService deliveryService;

    @PutMapping
    public DeliveryDto planDelivery(@RequestBody DeliveryDto deliveryDto) {
        return deliveryService.planDelivery(deliveryDto);
    }

    @PostMapping("/cost")
    public BigDecimal deliveryCost(@RequestBody OrderDto order) {
        return deliveryService.calculateDeliveryCost(order);
    }

    @PostMapping("/picked")
    public void deliveryPicked(@RequestBody UUID orderId) {
        deliveryService.pickDelivery(orderId);
    }

    @PostMapping("/successful")
    public void deliverySuccessful(@RequestBody UUID orderId) {
        deliveryService.successfulDelivery(orderId);
    }

    @PostMapping("/failed")
    public void deliveryFailed(@RequestBody UUID orderId) {
        deliveryService.failedDelivery(orderId);
    }
}