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
import ru.yandex.practicum.analyzer.serialization.HubEventDeserializer;
import ru.yandex.practicum.analyzer.service.HubEventService;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private static final List<String> TOPICS = List.of("telemetry.hubs.v1");
    private static final String GROUP_ID = "hub-analyzer-group";

    private final KafkaClientConfigurationImpl<HubEventAvro> client;
    private final HubEventService hubEventService;
    private Consumer<String, HubEventAvro> consumer;
    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        this.consumer = client.initConsumer(GROUP_ID, HubEventDeserializer.class);
    }

    @Override
    public void run() {
        System.err.println(">>> [HubEventProcessor] started, subscribing to: " + TOPICS);
        try {
            consumer.subscribe(TOPICS);
            while (running && !Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    HubEventAvro event = record.value();
                    if (event != null) {
                        try {
                            System.err.println(">>> [HubEvent] received: " + event.getPayload().getClass().getSimpleName());
                            hubEventService.handleHubEvent(event);
                        } catch (Exception e) {
                            System.err.println("!!! [HubEvent] error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
                consumer.commitAsync();
            }
        } catch (WakeupException e) {
            System.err.println(">>> [HubEventProcessor] woken up");
        } finally {
            System.err.println(">>> [HubEventProcessor] stopped");
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