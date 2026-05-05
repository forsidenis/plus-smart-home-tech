package ru.yandex.practicum.analyzer.processor;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.client.KafkaClientConfigurationImpl;
import ru.yandex.practicum.analyzer.serialization.SensorsSnapshotDeserializer;
import ru.yandex.practicum.analyzer.service.SnapshotAnalysisService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private static final List<String> TOPICS = List.of("telemetry.snapshots.v1");
    private static final String GROUP_ID = "snapshot-analyzer-group";

    private final KafkaClientConfigurationImpl<SensorsSnapshotAvro> client;
    private final SnapshotAnalysisService analysisService;
    private Consumer<String, SensorsSnapshotAvro> consumer;
    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        this.consumer = client.initConsumer(GROUP_ID, SensorsSnapshotDeserializer.class);
    }

    public void start() {
        try {
            consumer.subscribe(TOPICS);
            while (running && !Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    SensorsSnapshotAvro snapshot = record.value();
                    if (snapshot != null) {
                        try {
                            analysisService.processSnapshot(snapshot);
                        } catch (Exception e) {
                            log.error("Failed to process snapshot for hub {}", snapshot.getHubId(), e);
                        }
                    }
                }
                consumer.commitSync();
            }
        } catch (WakeupException e) {
            log.info("SnapshotProcessor woken up");
        } catch (Exception e) {
            log.error("Unexpected error in snapshot processing loop", e);
        } finally {
            log.info("SnapshotProcessor stopped");
            consumer.close();
        }
    }

    public void stop() {
        running = false;
        if (consumer != null) {
            consumer.wakeup();
        }
    }
}