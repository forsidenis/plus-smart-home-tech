package ru.yandex.practicum.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.dto.hub.HubEventDto;
import ru.yandex.practicum.telemetry.collector.dto.sensor.SensorEventDto;
import ru.yandex.practicum.telemetry.collector.mapper.HubEventMapper;
import ru.yandex.practicum.telemetry.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.telemetry.collector.util.AvroSerializer;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;
    private final AvroSerializer avroSerializer;

    @Value("${collector.kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${collector.kafka.topics.hubs}")
    private String hubsTopic;

    public void processSensorEvent(SensorEventDto event) {
        log.debug("Processing sensor event: {}", event);
        SensorEventAvro avroEvent = sensorEventMapper.toAvro(event);
        byte[] serialized = avroSerializer.serialize(avroEvent);

        kafkaTemplate.send(sensorsTopic, event.getHubId(), serialized)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send sensor event", ex);
                    } else {
                        log.info("Sensor event sent: hubId={}, sensorId={}",
                                event.getHubId(), avroEvent.getId());
                    }
                });
        kafkaTemplate.flush();
    }

    public void processHubEvent(HubEventDto event) {
        log.debug("Processing hub event: {}", event);
        HubEventAvro avroEvent = hubEventMapper.toAvro(event);
        byte[] serialized = avroSerializer.serialize(avroEvent);

        kafkaTemplate.send(hubsTopic, event.getHubId(), serialized)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send hub event", ex);
                    } else {
                        log.info("Hub event sent: hubId={}", event.getHubId());
                    }
                });
        kafkaTemplate.flush();
    }
}