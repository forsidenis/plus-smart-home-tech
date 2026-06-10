package ru.yandex.practicum.commerce.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.commerce.api.dto.OrderDto;
import ru.yandex.practicum.commerce.api.dto.PaymentDto;

@FeignClient(name = "payment")
public interface PaymentClient {
    @PostMapping("/api/v1/payment/totalCost")
    Double getTotalCost(@RequestBody OrderDto orderDto);

    @PostMapping("/api/v1/payment/productCost")
    Double productCost(@RequestBody OrderDto orderDto);

    @PostMapping("/api/v1/payment")
    PaymentDto payment(@RequestBody OrderDto orderDto);
}
