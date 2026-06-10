package ru.yandex.practicum.commerce.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.commerce.api.dto.OrderDto;

import java.util.UUID;

@FeignClient(name = "order")
public interface OrderClient {

    @PostMapping("/api/v1/order/payment")
    OrderDto payment(@RequestParam("orderId") UUID orderId,
                     @RequestParam("paymentId") UUID paymentId);

    @PostMapping("/api/v1/order/payment/failed")
    OrderDto paymentFailed(@RequestParam("orderId") UUID orderId);

    @PostMapping("/api/v1/order/delivery")
    OrderDto delivery(@RequestParam("orderId") UUID orderId,
                      @RequestParam("deliveryId") UUID deliveryId);

    @PostMapping("/api/v1/order/delivery/failed")
    OrderDto deliveryFailed(@RequestParam("orderId") UUID orderId);
}