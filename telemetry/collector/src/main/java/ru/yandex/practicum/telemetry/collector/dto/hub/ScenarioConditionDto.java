package ru.yandex.practicum.telemetry.collector.dto.hub;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class ScenarioConditionDto {
    private String sensorId;
    private ConditionTypeDto type;
    private ConditionOperationDto operation;
    private Integer value;
}