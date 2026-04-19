package ru.yandex.practicum.telemetry.collector.dto.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString(callSuper = true)
public class DeviceAddedEventDto extends HubEventDto {
    @NotBlank
    private String id;
    @NotNull
    private DeviceTypeDto deviceType;

    @Override
    public HubEventType getType() {
        return HubEventType.DEVICE_ADDED;
    }
}