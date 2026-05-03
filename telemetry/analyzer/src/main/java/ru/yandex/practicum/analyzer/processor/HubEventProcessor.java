package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.service.HubEventService;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final ConsumerFactory<String, HubEventAvro> consumerFactory;
    private final HubEventService hubEventService;

    @Value("${analyzer.kafka.topics.hubs}")
    private String hubsTopic;

    @Override
    public void run() {
        try (Consumer<String, HubEventAvro> consumer = consumerFactory.createConsumer()) {
            consumer.subscribe(Collections.singletonList(hubsTopic));
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    HubEventAvro event = record.value();
                    if (event != null) {
                        hubEventService.handleHubEvent(event);
                    }
                }
                consumer.commitAsync();
            }
        } catch (org.apache.kafka.common.errors.WakeupException e) {
            log.info("HubEventProcessor woken up");
        } finally {
            log.info("HubEventProcessor stopped");
        }
    }
}
