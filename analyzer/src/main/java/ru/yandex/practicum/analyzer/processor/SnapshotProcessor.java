package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.service.SnapshotAnalysisService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final ConsumerFactory<String, SensorsSnapshotAvro> consumerFactory;
    private final SnapshotAnalysisService analysisService;

    @Value("${analyzer.kafka.topics.snapshots}")
    private String snapshotsTopic;

    private volatile boolean running = true;

    public void start() {
        try (Consumer<String, SensorsSnapshotAvro> consumer = consumerFactory.createConsumer()) {
            consumer.subscribe(Collections.singletonList(snapshotsTopic));
            while (running && !Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    SensorsSnapshotAvro snapshot = record.value();
                    if (snapshot != null) {
                        analysisService.processSnapshot(snapshot);
                    }
                }
                consumer.commitSync();
            }
        } catch (org.apache.kafka.common.errors.WakeupException e) {
            log.info("SnapshotProcessor woken up");
        } finally {
            log.info("SnapshotProcessor stopped");
        }
    }

    public void stop() {
        running = false;
    }
}