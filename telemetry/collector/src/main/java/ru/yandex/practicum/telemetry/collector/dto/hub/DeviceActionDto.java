package ru.yandex.practicum.telemetry.collector.dto.hub;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class DeviceActionDto {
    private String sensorId;
    private ActionTypeDto type;
    private Integer value;
}