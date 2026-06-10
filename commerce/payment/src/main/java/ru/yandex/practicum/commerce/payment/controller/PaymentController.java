package ru.yandex.practicum.commerce.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.commerce.api.dto.OrderDto;
import ru.yandex.practicum.commerce.api.dto.PaymentDto;
import ru.yandex.practicum.commerce.payment.service.PaymentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/productCost")
    public Double productCost(@RequestBody OrderDto order) {
        return paymentService.calculateProductCost(order);
    }

    @PostMapping("/totalCost")
    public Double getTotalCost(@RequestBody OrderDto order) {
        return paymentService.calculateTotalCost(order);
    }

    @PostMapping
    public PaymentDto payment(@RequestBody OrderDto order) {
        return paymentService.createPayment(order);
    }

    @PostMapping("/refund")
    public void paymentSuccess(@RequestBody UUID paymentId) {
        paymentService.paymentSuccess(paymentId);
    }

    @PostMapping("/failed")
    public void paymentFailed(@RequestBody UUID paymentId) {
        paymentService.paymentFailed(paymentId);
    }
}
