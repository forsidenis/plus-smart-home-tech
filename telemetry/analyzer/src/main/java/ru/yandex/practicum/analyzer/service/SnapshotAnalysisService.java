package ru.yandex.practicum.analyzer.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.analyzer.entity.*;
import ru.yandex.practicum.analyzer.repository.*;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotAnalysisService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public void processSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        log.debug("Analyzing snapshot for hub {}", hubId);

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        for (Scenario scenario : scenarios) {
            if (checkConditions(snapshot, scenario)) {
                executeActions(snapshot, scenario);
            }
        }
    }

    private boolean checkConditions(SensorsSnapshotAvro snapshot, Scenario scenario) {
        List<ScenarioCondition> conditions = scenarioConditionRepository.findByScenarioId(scenario.getId());
        for (ScenarioCondition sc : conditions) {
            String sensorId = sc.getSensor().getId();
            SensorStateAvro state = snapshot.getSensorsState().get(sensorId);
            if (state == null) return false;
            Condition cond = sc.getCondition();
            Object data = state.getData();

            int sensorValue = extractValue(data, cond.getType());
            boolean result = switch (cond.getOperation()) {
                case "EQUALS" -> sensorValue == cond.getValue();
                case "GREATER_THAN" -> sensorValue > cond.getValue();
                case "LOWER_THAN" -> sensorValue < cond.getValue();
                default -> false;
            };
            if (!result) return false;
        }
        return true;
    }

    private int extractValue(Object data, String type) {
        return switch (type) {
            case "TEMPERATURE" -> {
                if (data instanceof TemperatureSensorAvro t) {
                    yield t.getTemperatureC();
                } else if (data instanceof ClimateSensorAvro c) {
                    yield c.getTemperatureC();
                }
                throw new IllegalArgumentException("Unsupported data type for TEMPERATURE: " + data.getClass());
            }
            case "HUMIDITY" -> ((ClimateSensorAvro) data).getHumidity();
            case "CO2LEVEL" -> ((ClimateSensorAvro) data).getCo2Level();
            case "LUMINOSITY" -> ((LightSensorAvro) data).getLuminosity();
            case "MOTION" -> ((MotionSensorAvro) data).getMotion() ? 1 : 0;
            case "SWITCH" -> ((SwitchSensorAvro) data).getState() ? 1 : 0;
            default -> throw new IllegalArgumentException("Unknown condition type: " + type);
        };
    }

    private void executeActions(SensorsSnapshotAvro snapshot, Scenario scenario) {
        List<ScenarioAction> actions = scenarioActionRepository.findByScenarioId(scenario.getId());
        for (ScenarioAction sa : actions) {
            Action action = sa.getAction();
            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(snapshot.getHubId())
                    .setScenarioName(scenario.getName())
                    .setAction(mapToProtoAction(action, sa.getSensor().getId()))
                    .setTimestamp(toProtoTimestamp(snapshot.getTimestamp()))
                    .build();

            hubRouterClient.handleDeviceAction(request);
            log.info("Sent action {} for hub {}", action.getType(), snapshot.getHubId());
        }
    }

    private DeviceActionProto mapToProtoAction(Action action, String sensorId) {
        return DeviceActionProto.newBuilder()
                .setSensorId(sensorId)
                .setType(ActionTypeProto.valueOf(action.getType()))
                .setValue(action.getValue())
                .build();
    }

    private Timestamp toProtoTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}