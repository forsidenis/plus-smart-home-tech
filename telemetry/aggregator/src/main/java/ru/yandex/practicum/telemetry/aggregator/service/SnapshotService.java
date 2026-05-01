package ru.yandex.practicum.telemetry.aggregator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SnapshotService {
    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);

    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public synchronized Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();

        SensorsSnapshotAvro snapshot = snapshots.get(hubId);
        if (snapshot == null) {
            snapshot = SensorsSnapshotAvro.newBuilder()
                    .setHubId(hubId)
                    .setTimestamp(event.getTimestamp())
                    .setSensorsState(new HashMap<>())
                    .build();
            updateSnapshot(snapshot, event);
            snapshots.put(hubId, snapshot);
            log.debug("Created new snapshot for hub {}", hubId);
            return Optional.of(snapshot);
        }

        SensorStateAvro oldState = snapshot.getSensorsState().get(sensorId);
        if (oldState != null) {
            if (event.getTimestamp().isBefore(oldState.getTimestamp())) {
                log.debug("Ignoring older event for sensor {} (hub {})", sensorId, hubId);
                return Optional.empty();
            }
            if (payloadEquals(oldState.getData(), event.getPayload())) {
                log.debug("Data unchanged for sensor {} (hub {})", sensorId, hubId);
                return Optional.empty();
            }
        }

        updateSnapshot(snapshot, event);
        log.debug("Updated snapshot for hub {} with sensor {}", hubId, sensorId);
        return Optional.of(snapshot);
    }

    private void updateSnapshot(SensorsSnapshotAvro snapshot, SensorEventAvro event) {
        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();
        snapshot.getSensorsState().put(event.getId(), newState);
        snapshot.setTimestamp(event.getTimestamp());
    }

    private boolean payloadEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}