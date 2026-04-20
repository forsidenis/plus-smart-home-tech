package ru.yandex.practicum.telemetry.collector.dto.hub;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.yandex.practicum.telemetry.collector.enumeration.ConditionOperation;
import ru.yandex.practicum.telemetry.collector.enumeration.ConditionType;

@Getter
@Setter
@ToString
public class ScenarioConditionDto {

    private String sensorId;

    private ConditionType type;

    private ConditionOperation operation;

    private Integer value;
}