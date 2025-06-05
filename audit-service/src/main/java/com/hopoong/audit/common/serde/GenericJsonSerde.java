package com.hopoong.audit.common.serde;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class GenericJsonSerde<T> implements Serde<T> {

    private final ObjectMapper objectMapper;
    private final TypeReference<T> typeReference;

    public GenericJsonSerde(ObjectMapper objectMapper, TypeReference<T> typeReference) {
        this.objectMapper = objectMapper;
        this.typeReference = typeReference;
    }

    @Override
    public Serializer<T> serializer() {
        return new Serializer<>() {
            private final StringSerializer stringSerializer = new StringSerializer();

            @Override
            public byte[] serialize(String topic, T data) {
                try {
                    String json = objectMapper.writeValueAsString(data);
                    return stringSerializer.serialize(topic, json);
                } catch (Exception e) {
                    throw new RuntimeException("JSON serialization failed", e);
                }
            }
        };
    }

    @Override
    public Deserializer<T> deserializer() {
        return new Deserializer<>() {
            private final StringDeserializer stringDeserializer = new StringDeserializer();

            @Override
            public T deserialize(String topic, byte[] data) {
                try {
                    String value = stringDeserializer.deserialize(topic, data);

                    if (value == null) return null;

                    String jsonString = objectMapper.readValue(value, String.class);
                    return objectMapper.readValue(jsonString, typeReference);
                } catch (Exception e) {
                    throw new RuntimeException("JSON deserialization failed", e);
                }
            }
        };
    }
}
