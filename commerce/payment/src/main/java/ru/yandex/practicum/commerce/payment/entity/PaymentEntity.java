package ru.yandex.practicum.commerce.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.commerce.api.dto.PaymentState;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentEntity {
    @Id
    private UUID paymentId;
    private UUID orderId;
    private Double totalPayment;
    private Double deliveryTotal;
    private Double feeTotal;
    private Double productTotal;
    @Enumerated(EnumType.STRING)
    private PaymentState state;
}