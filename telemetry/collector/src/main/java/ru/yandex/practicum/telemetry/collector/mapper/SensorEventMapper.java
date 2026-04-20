package ru.yandex.practicum.telemetry.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.collector.dto.sensor.*;

@Component
public class SensorEventMapper {

    public SensorEventAvro toAvro(SensorEventDto event) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp());

        Object payload = switch (event.getType()) {
            case CLIMATE_SENSOR_EVENT -> mapClimateSensor((ClimateSensorEventDto) event);
            case LIGHT_SENSOR_EVENT -> mapLightSensor((LightSensorEventDto) event);
            case MOTION_SENSOR_EVENT -> mapMotionSensor((MotionSensorEventDto) event);
            case SWITCH_SENSOR_EVENT -> mapSwitchSensor((SwitchSensorEventDto) event);
            case TEMPERATURE_SENSOR_EVENT -> mapTemperatureSensor((TemperatureSensorEventDto) event);
        };

        builder.setPayload(payload);
        return builder.build();
    }

    private ClimateSensorAvro mapClimateSensor(ClimateSensorEventDto dto) {
        return ClimateSensorAvro.newBuilder()
                .setTemperatureC(dto.getTemperatureC())
                .setHumidity(dto.getHumidity())
                .setCo2Level(dto.getCo2Level())
                .build();
    }

    private LightSensorAvro mapLightSensor(LightSensorEventDto dto) {
        return LightSensorAvro.newBuilder()
                .setLinkQuality(dto.getLinkQuality() != null ? dto.getLinkQuality() : 0)
                .setLuminosity(dto.getLuminosity() != null ? dto.getLuminosity() : 0)
                .build();
    }

    private MotionSensorAvro mapMotionSensor(MotionSensorEventDto dto) {
        return MotionSensorAvro.newBuilder()
                .setLinkQuality(dto.getLinkQuality())
                .setMotion(dto.getMotion())
                .setVoltage(dto.getVoltage())
                .build();
    }

    private SwitchSensorAvro mapSwitchSensor(SwitchSensorEventDto dto) {
        return SwitchSensorAvro.newBuilder()
                .setState(dto.getState())
                .build();
    }

    private TemperatureSensorAvro mapTemperatureSensor(TemperatureSensorEventDto dto) {
        return TemperatureSensorAvro.newBuilder()
                .setTemperatureC(dto.getTemperatureC())
                .setTemperatureF(dto.getTemperatureF())
                .build();
    }
}