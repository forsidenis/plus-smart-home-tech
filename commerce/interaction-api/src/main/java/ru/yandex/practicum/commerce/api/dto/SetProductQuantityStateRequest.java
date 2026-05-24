package ru.yandex.practicum.commerce.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetProductQuantityStateRequest {
    private UUID productId;
    private QuantityState quantityState;
}