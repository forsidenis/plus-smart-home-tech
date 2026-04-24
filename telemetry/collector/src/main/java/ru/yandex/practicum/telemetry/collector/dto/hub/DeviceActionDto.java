package ru.yandex.practicum.telemetry.collector.dto.hub;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.yandex.practicum.telemetry.collector.enumeration.ActionType;

@Getter
@Setter
@ToString
public class DeviceActionDto {
    private String sensorId;
    private ActionType type;
    private Integer value;
}