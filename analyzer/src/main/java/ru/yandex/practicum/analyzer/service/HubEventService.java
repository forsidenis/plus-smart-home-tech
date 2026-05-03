package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.entity.*;
import ru.yandex.practicum.analyzer.repository.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class HubEventService {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    @Transactional
    public void handleHubEvent(HubEventAvro event) {
        Object payload = event.getPayload();
        switch (payload) {
            case DeviceAddedEventAvro da -> handleDeviceAdded(event.getHubId(), da);
            case DeviceRemovedEventAvro dr -> handleDeviceRemoved(event.getHubId(), dr);
            case ScenarioAddedEventAvro sa -> handleScenarioAdded(event.getHubId(), sa);
            case ScenarioRemovedEventAvro sr -> handleScenarioRemoved(event.getHubId(), sr);
            default -> log.warn("Unknown hub event type: {}", payload.getClass());
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro event) {
        Sensor sensor = Sensor.builder()
                .id(event.getId())
                .hubId(hubId)
                .build();
        sensorRepository.save(sensor);
        log.debug("Added sensor {} for hub {}", event.getId(), hubId);
    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro event) {
        sensorRepository.deleteById(event.getId());
        log.debug("Removed sensor {} from hub {}", event.getId(), hubId);
    }

    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro event) {
        scenarioRepository.findByHubIdAndName(hubId, event.getName())
                .ifPresent(s -> deleteScenario(s.getId()));

        Scenario scenario = Scenario.builder()
                .hubId(hubId)
                .name(event.getName())
                .build();
        scenario = scenarioRepository.save(scenario);

        for (ScenarioConditionAvro condAvro : event.getConditions()) {
            Condition condition = Condition.builder()
                    .type(condAvro.getType().name())
                    .operation(condAvro.getOperation().name())
                    .value((Integer) condAvro.getValue())
                    .build();
            condition = conditionRepository.save(condition);

            Sensor sensor = sensorRepository.findByIdAndHubId(condAvro.getSensorId(), hubId)
                    .orElseThrow(() -> new RuntimeException("Sensor not found: " + condAvro.getSensorId()));

            ScenarioConditionId scId = new ScenarioConditionId(scenario.getId(), sensor.getId(), condition.getId());
            ScenarioCondition sc = ScenarioCondition.builder()
                    .id(scId)
                    .scenario(scenario)
                    .sensor(sensor)
                    .condition(condition)
                    .build();
            scenarioConditionRepository.save(sc);
        }

        for (DeviceActionAvro actionAvro : event.getActions()) {
            Action action = Action.builder()
                    .type(actionAvro.getType().name())
                    .value(actionAvro.getValue())
                    .build();
            action = actionRepository.save(action);

            Sensor sensor = sensorRepository.findByIdAndHubId(actionAvro.getSensorId(), hubId)
                    .orElseThrow(() -> new RuntimeException("Sensor not found: " + actionAvro.getSensorId()));

            ScenarioActionId saId = new ScenarioActionId(scenario.getId(), sensor.getId(), action.getId());
            ScenarioAction sa = ScenarioAction.builder()
                    .id(saId)
                    .scenario(scenario)
                    .sensor(sensor)
                    .action(action)
                    .build();
            scenarioActionRepository.save(sa);
        }

        log.debug("Added scenario '{}' for hub {}", event.getName(), hubId);
    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro event) {
        scenarioRepository.findByHubIdAndName(hubId, event.getName())
                .ifPresent(s -> deleteScenario(s.getId()));
    }

    private void deleteScenario(Long scenarioId) {
        scenarioConditionRepository.deleteByScenarioId(scenarioId);
        scenarioActionRepository.deleteByScenarioId(scenarioId);
        scenarioRepository.deleteById(scenarioId);
    }
}