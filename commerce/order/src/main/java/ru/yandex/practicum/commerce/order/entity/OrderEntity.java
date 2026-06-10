package ru.yandex.practicum.commerce.order.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.commerce.api.dto.AddressDto;
import ru.yandex.practicum.commerce.api.dto.OrderState;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {
    @Id
    private UUID orderId;
    private UUID shoppingCartId;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_products", joinColumns = @JoinColumn(name = "order_id"))
    @MapKeyColumn(name = "product_id")
    @Column(name = "quantity")
    private Map<UUID, Long> products;
    private UUID paymentId;
    private UUID deliveryId;
    @Enumerated(EnumType.STRING)
    private OrderState state;
    private Double deliveryWeight;
    private Double deliveryVolume;
    private Boolean fragile;
    private Double totalPrice;
    private Double deliveryPrice;
    private Double productPrice;
    @Embedded
    private AddressDto deliveryAddress;
    private String username;
}