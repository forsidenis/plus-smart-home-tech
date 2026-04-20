package ru.yandex.practicum.telemetry.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.collector.dto.hub.*;
import ru.yandex.practicum.telemetry.collector.enumeration.*;

import java.util.stream.Collectors;

@Component
public class HubEventMapper {

    public HubEventAvro toAvro(HubEventDto event) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp());

        Object payload = switch (event.getType()) {
            case DEVICE_ADDED -> mapDeviceAdded((DeviceAddedEventDto) event);
            case DEVICE_REMOVED -> mapDeviceRemoved((DeviceRemovedEventDto) event);
            case SCENARIO_ADDED -> mapScenarioAdded((ScenarioAddedEventDto) event);
            case SCENARIO_REMOVED -> mapScenarioRemoved((ScenarioRemovedEventDto) event);
        };

        builder.setPayload(payload);
        return builder.build();
    }

    private DeviceAddedEventAvro mapDeviceAdded(DeviceAddedEventDto dto) {
        return DeviceAddedEventAvro.newBuilder()
                .setId(dto.getId())
                .setType(mapDeviceType(dto.getDeviceType()))
                .build();
    }

    private DeviceRemovedEventAvro mapDeviceRemoved(DeviceRemovedEventDto dto) {
        return DeviceRemovedEventAvro.newBuilder()
                .setId(dto.getId())
                .build();
    }

    private ScenarioAddedEventAvro mapScenarioAdded(ScenarioAddedEventDto dto) {
        return ScenarioAddedEventAvro.newBuilder()
                .setName(dto.getName())
                .setConditions(dto.getConditions().stream()
                        .map(this::mapCondition)
                        .collect(Collectors.toList()))
                .setActions(dto.getActions().stream()
                        .map(this::mapAction)
                        .collect(Collectors.toList()))
                .build();
    }

    private ScenarioRemovedEventAvro mapScenarioRemoved(ScenarioRemovedEventDto dto) {
        return ScenarioRemovedEventAvro.newBuilder()
                .setName(dto.getName())
                .build();
    }

    private DeviceTypeAvro mapDeviceType(DeviceType type) {
        return switch (type) {
            case MOTION_SENSOR -> DeviceTypeAvro.MOTION_SENSOR;
            case TEMPERATURE_SENSOR -> DeviceTypeAvro.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR -> DeviceTypeAvro.LIGHT_SENSOR;
            case CLIMATE_SENSOR -> DeviceTypeAvro.CLIMATE_SENSOR;
            case SWITCH_SENSOR -> DeviceTypeAvro.SWITCH_SENSOR;
        };
    }

    private ScenarioConditionAvro mapCondition(ScenarioConditionDto dto) {
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                .setSensorId(dto.getSensorId())
                .setType(mapConditionType(dto.getType()))
                .setOperation(mapConditionOperation(dto.getOperation()));
        if (dto.getValue() != null) {
            builder.setValue(dto.getValue());
        }
        return builder.build();
    }

    private ConditionTypeAvro mapConditionType(ConditionType type) {
        return switch (type) {
            case MOTION -> ConditionTypeAvro.MOTION;
            case LUMINOSITY -> ConditionTypeAvro.LUMINOSITY;
            case SWITCH -> ConditionTypeAvro.SWITCH;
            case TEMPERATURE -> ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL -> ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY -> ConditionTypeAvro.HUMIDITY;
        };
    }

    private ConditionOperationAvro mapConditionOperation(ConditionOperation op) {
        return switch (op) {
            case EQUALS -> ConditionOperationAvro.EQUALS;
            case GREATER_THAN -> ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN -> ConditionOperationAvro.LOWER_THAN;
        };
    }

    private DeviceActionAvro mapAction(DeviceActionDto dto) {
        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder()
                .setSensorId(dto.getSensorId())
                .setType(mapActionType(dto.getType()));
        if (dto.getValue() != null) {
            builder.setValue(dto.getValue());
        }
        return builder.build();
    }

    private ActionTypeAvro mapActionType(ActionType type) {
        return switch (type) {
            case ACTIVATE -> ActionTypeAvro.ACTIVATE;
            case DEACTIVATE -> ActionTypeAvro.DEACTIVATE;
            case INVERSE -> ActionTypeAvro.INVERSE;
            case SET_VALUE -> ActionTypeAvro.SET_VALUE;
        };
    }
}