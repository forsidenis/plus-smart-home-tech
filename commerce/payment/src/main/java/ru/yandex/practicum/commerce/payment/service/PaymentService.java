package ru.yandex.practicum.commerce.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.api.client.OrderClient;
import ru.yandex.practicum.commerce.api.client.ShoppingStoreClient;
import ru.yandex.practicum.commerce.api.dto.OrderDto;
import ru.yandex.practicum.commerce.api.dto.PaymentDto;
import ru.yandex.practicum.commerce.api.dto.PaymentState;
import ru.yandex.practicum.commerce.api.dto.ProductDto;
import ru.yandex.practicum.commerce.payment.entity.PaymentEntity;
import ru.yandex.practicum.commerce.payment.repository.PaymentRepository;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ShoppingStoreClient shoppingStoreClient;
    private final OrderClient orderClient;

    public Double calculateProductCost(OrderDto order) {
        double total = 0.0;
        for (Map.Entry<UUID, Long> entry : order.getProducts().entrySet()) {
            ProductDto product = shoppingStoreClient.getProduct(entry.getKey());
            total += product.getPrice() * entry.getValue();
        }
        return total;
    }

    public Double calculateTotalCost(OrderDto order) {
        double productCost = calculateProductCost(order);
        double tax = productCost * 0.1;
        double delivery = order.getDeliveryPrice() != null ? order.getDeliveryPrice() : 0.0;
        return productCost + tax + delivery;
    }

    @Transactional
    public PaymentDto createPayment(OrderDto order) {
        double productCost = calculateProductCost(order);
        double fee = productCost * 0.1;
        double delivery = order.getDeliveryPrice() != null ? order.getDeliveryPrice() : 0.0;
        double total = productCost + fee + delivery;

        PaymentEntity payment = PaymentEntity.builder()
                .paymentId(UUID.randomUUID())
                .orderId(order.getOrderId())
                .totalPayment(total)
                .deliveryTotal(delivery)
                .feeTotal(fee)
                .productTotal(productCost)
                .state(PaymentState.PENDING)
                .build();
        paymentRepository.save(payment);

        return PaymentDto.builder()
                .paymentId(payment.getPaymentId())
                .totalPayment(payment.getTotalPayment())
                .deliveryTotal(payment.getDeliveryTotal())
                .feeTotal(payment.getFeeTotal())
                .build();
    }

    @Transactional
    public void paymentSuccess(UUID paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setState(PaymentState.SUCCESS);
        paymentRepository.save(payment);
        orderClient.payment(payment.getOrderId(), paymentId); // вызов order с paymentId
    }

    @Transactional
    public void paymentFailed(UUID paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setState(PaymentState.FAILED);
        paymentRepository.save(payment);
        orderClient.paymentFailed(payment.getOrderId());
    }
}
