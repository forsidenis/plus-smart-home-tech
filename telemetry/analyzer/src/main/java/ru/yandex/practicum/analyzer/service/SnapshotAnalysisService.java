package ru.yandex.practicum.analyzer.service;

import com.google.protobuf.Timestamp;
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
@Slf4j
public class SnapshotAnalysisService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public SnapshotAnalysisService(ScenarioRepository scenarioRepository,
                                   ScenarioConditionRepository scenarioConditionRepository,
                                   ScenarioActionRepository scenarioActionRepository,
                                   @GrpcClient("hub-router") HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient) {
        this.scenarioRepository = scenarioRepository;
        this.scenarioConditionRepository = scenarioConditionRepository;
        this.scenarioActionRepository = scenarioActionRepository;
        this.hubRouterClient = hubRouterClient;
    }

    public void processSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        log.info("[SnapshotAnalysis] processing snapshot for hub {}, sensors: {}", hubId, snapshot.getSensorsState().keySet());

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        log.debug("[SnapshotAnalysis] found {} scenarios for hub {}", scenarios.size(), hubId);

        for (Scenario scenario : scenarios) {
            log.debug("[SnapshotAnalysis] checking scenario '{}'", scenario.getName());
            if (checkConditions(snapshot, scenario)) {
                log.info("[SnapshotAnalysis] conditions MET for scenario '{}'", scenario.getName());
                executeActions(snapshot, scenario);
            } else {
                log.debug("[SnapshotAnalysis] conditions NOT met for scenario '{}'", scenario.getName());
            }
        }
    }

    private boolean checkConditions(SensorsSnapshotAvro snapshot, Scenario scenario) {
        List<ScenarioCondition> conditions = scenarioConditionRepository.findByScenarioId(scenario.getId());
        if (conditions.isEmpty()) {
            log.debug("No conditions found for scenario {}", scenario.getName());
            return false;
        }
        for (ScenarioCondition sc : conditions) {
            String sensorId = sc.getSensor().getId();
            SensorStateAvro state = snapshot.getSensorsState().get(sensorId);
            if (state == null) {
                log.debug("Sensor {} not in snapshot, skipping scenario", sensorId);
                return false;
            }
            Condition cond = sc.getCondition();
            Object data = state.getData();
            if (data == null) {
                log.debug("Sensor {} data is null", sensorId);
                return false;
            }

            int sensorValue = extractValue(data, cond.getType());
            boolean result = switch (cond.getOperation()) {
                case "EQUALS" -> sensorValue == cond.getValue();
                case "GREATER_THAN" -> sensorValue > cond.getValue();
                case "LOWER_THAN" -> sensorValue < cond.getValue();
                default -> false;
            };
            log.debug("Condition check: {} {} {} (actual={}) -> {}", cond.getType(), cond.getOperation(), cond.getValue(), sensorValue, result);
            if (!result) return false;
        }
        return true;
    }

    private int extractValue(Object data, String type) {
        return switch (type) {
            case "TEMPERATURE" -> {
                if (data instanceof ClimateSensorAvro c) {
                    yield c.getTemperatureC();
                } else if (data instanceof TemperatureSensorAvro t) {
                    yield t.getTemperatureC();
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
        log.info("Executing {} actions for scenario '{}'", actions.size(), scenario.getName());
        for (ScenarioAction sa : actions) {
            Action action = sa.getAction();
            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(snapshot.getHubId())
                    .setScenarioName(scenario.getName())
                    .setAction(mapToProtoAction(action, sa.getSensor().getId()))
                    .setTimestamp(toProtoTimestamp(snapshot.getTimestamp()))
                    .build();

            try {
                log.debug("[gRPC] sending action: {}", request);
                hubRouterClient.handleDeviceAction(request);
                log.debug("[gRPC] action sent");
            } catch (Exception e) {
                log.error("[gRPC] error: {}", e.getMessage(), e);
            }
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