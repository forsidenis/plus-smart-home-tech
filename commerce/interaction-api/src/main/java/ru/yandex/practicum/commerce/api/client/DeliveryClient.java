package ru.yandex.practicum.commerce.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.commerce.api.dto.DeliveryDto;
import ru.yandex.practicum.commerce.api.dto.OrderDto;

import java.math.BigDecimal;

@FeignClient(name = "delivery")
public interface DeliveryClient {
    @PutMapping("/api/v1/delivery")
    DeliveryDto planDelivery(@RequestBody DeliveryDto deliveryDto);

    @PostMapping("/api/v1/delivery/cost")
    BigDecimal deliveryCost(@RequestBody OrderDto orderDto);
}