package ru.yandex.practicum.analyzer.client;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import ru.yandex.practicum.analyzer.serialization.BaseAvroDeserializer;

public interface ClientConfiguration<T extends SpecificRecordBase> {
    Consumer<String, T> initConsumer(String groupId, Class<? extends BaseAvroDeserializer<T>> deserializer);
}