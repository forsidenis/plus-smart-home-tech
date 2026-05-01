package ru.yandex.practicum.telemetry.aggregator.starter;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.aggregator.serialization.AvroSerializer;
import ru.yandex.practicum.telemetry.aggregator.service.SnapshotService;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

@Component
public class AggregationStarter {
    private static final Logger log = LoggerFactory.getLogger(AggregationStarter.class);

    private final ConsumerFactory<String, SensorEventAvro> consumerFactory;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final SnapshotService snapshotService;

    @Value("${aggregator.kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${aggregator.kafka.topics.snapshots}")
    private String snapshotsTopic;

    @Autowired
    public AggregationStarter(ConsumerFactory<String, SensorEventAvro> consumerFactory,
                              KafkaTemplate<String, byte[]> kafkaTemplate,
                              SnapshotService snapshotService) {
        this.consumerFactory = consumerFactory;
        this.kafkaTemplate = kafkaTemplate;
        this.snapshotService = snapshotService;
    }

    public void start() {
        Consumer<String, SensorEventAvro> consumer = consumerFactory.createConsumer();
        try {
            consumer.subscribe(Collections.singletonList(sensorsTopic));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records =
                        consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    SensorEventAvro event = record.value();
                    if (event == null) continue;

                    Optional<SensorsSnapshotAvro> updatedSnapshot =
                            snapshotService.updateState(event);
                    if (updatedSnapshot.isPresent()) {
                        byte[] serialized = AvroSerializer.serialize(updatedSnapshot.get());
                        kafkaTemplate.send(snapshotsTopic, event.getHubId(), serialized)
                                .whenComplete((result, ex) -> {
                                    if (ex != null) {
                                        log.error("Failed to send snapshot for hub {}",
                                                event.getHubId(), ex);
                                    } else {
                                        log.debug("Snapshot sent for hub {}",
                                                event.getHubId());
                                    }
                                });
                    }
                }
                consumer.commitSync();
            }
        } catch (WakeupException e) {
            log.info("Wakeup signal received, closing consumer...");
        } catch (Exception e) {
            log.error("Unexpected error in aggregation loop", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
                kafkaTemplate.flush();
            }
        }
    }
}