package ru.yandex.practicum.commerce.delivery.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.commerce.api.dto.DeliveryState;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "deliveries")
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeliveryEntity that)) return false;
        return deliveryId != null && deliveryId.equals(that.deliveryId);
    }

    @Override
    public int hashCode() {
        return deliveryId != null ? deliveryId.hashCode() : 0;
    }
}