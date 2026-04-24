package ru.yandex.practicum.telemetry.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

@Component
public class SensorEventProtoMapper {

    public SensorEventAvro toAvro(SensorEventProto event) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(convertTimestamp(event.getTimestamp()));

        Object payload = switch (event.getPayloadCase()) {
            case MOTION_SENSOR -> mapMotionSensor(event.getMotionSensor());
            case TEMPERATURE_SENSOR -> mapTemperatureSensor(event.getTemperatureSensor());
            case LIGHT_SENSOR -> mapLightSensor(event.getLightSensor());
            case CLIMATE_SENSOR -> mapClimateSensor(event.getClimateSensor());
            case SWITCH_SENSOR -> mapSwitchSensor(event.getSwitchSensor());
            default -> throw new IllegalArgumentException("Unknown sensor event type: " + event.getPayloadCase());
        };

        builder.setPayload(payload);
        return builder.build();
    }

    private Instant convertTimestamp(com.google.protobuf.Timestamp ts) {
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    }

    private MotionSensorAvro mapMotionSensor(MotionSensorProto proto) {
        return MotionSensorAvro.newBuilder()
                .setLinkQuality(proto.getLinkQuality())
                .setMotion(proto.getMotion())
                .setVoltage(proto.getVoltage())
                .build();
    }

    private TemperatureSensorAvro mapTemperatureSensor(TemperatureSensorProto proto) {
        return TemperatureSensorAvro.newBuilder()
                .setTemperatureC(proto.getTemperatureC())
                .setTemperatureF(proto.getTemperatureF())
                .build();
    }

    private LightSensorAvro mapLightSensor(LightSensorProto proto) {
        return LightSensorAvro.newBuilder()
                .setLinkQuality(proto.getLinkQuality())
                .setLuminosity(proto.getLuminosity())
                .build();
    }

    private ClimateSensorAvro mapClimateSensor(ClimateSensorProto proto) {
        return ClimateSensorAvro.newBuilder()
                .setTemperatureC(proto.getTemperatureC())
                .setHumidity(proto.getHumidity())
                .setCo2Level(proto.getCo2Level())
                .build();
    }

    private SwitchSensorAvro mapSwitchSensor(SwitchSensorProto proto) {
        return SwitchSensorAvro.newBuilder()
                .setState(proto.getState())
                .build();
    }
}