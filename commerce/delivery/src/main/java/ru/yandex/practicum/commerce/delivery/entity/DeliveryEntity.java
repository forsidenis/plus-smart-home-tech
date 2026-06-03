package ru.yandex.practicum.commerce.delivery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.commerce.api.dto.DeliveryState;

import java.util.UUID;

@Entity
@Table(name = "deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEntity {
    @Id
    private UUID deliveryId;

    private UUID orderId;

    @Embedded
    private AddressEntity fromAddress;

    @Embedded
    private AddressEntity toAddress;

    @Enumerated(EnumType.STRING)
    private DeliveryState deliveryState;
}