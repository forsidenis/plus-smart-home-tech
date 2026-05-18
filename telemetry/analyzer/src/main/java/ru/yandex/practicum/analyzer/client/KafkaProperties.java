package ru.yandex.practicum.analyzer.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {

    private String bootstrapServers;

    private ConsumerConfig consumer = new ConsumerConfig();

    @Data
    public static class ConsumerConfig {
        private String autoOffsetReset;
        private String keyDeserializer;
    }
}