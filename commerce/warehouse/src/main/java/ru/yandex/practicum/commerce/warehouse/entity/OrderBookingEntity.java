package ru.yandex.practicum.commerce.warehouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_bookings")
public class OrderBookingEntity {

    @Id
    @Builder.Default
    private UUID bookingId = UUID.randomUUID();

    @Column(unique = true, nullable = false)
    private UUID orderId;

    private UUID deliveryId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_products", joinColumns = @JoinColumn(name = "booking_id"))
    @MapKeyColumn(name = "product_id")
    @Column(name = "quantity")
    private Map<UUID, Long> products;
}