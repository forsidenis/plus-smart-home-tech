package ru.yandex.practicum.analyzer.serialization;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {
    private final DecoderFactory decoderFactory;
    private final Schema schema;

    public BaseAvroDeserializer(DecoderFactory decoderFactory, Schema schema) {
        this.decoderFactory = decoderFactory;
        this.schema = schema;
    }

    public BaseAvroDeserializer(Schema schema) {
        this(DecoderFactory.get(), schema);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            BinaryDecoder decoder = decoderFactory.binaryDecoder(bais, null);
            DatumReader<T> reader = new SpecificDatumReader<>(schema);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize Avro record", e);
        }
    }
}