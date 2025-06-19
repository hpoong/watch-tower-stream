package com.hopoong.audit.common.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.adapter.kafka.resourcemetric.model.AvgMax;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

public class AvgMaxSerde implements Serde<AvgMax> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Serializer<AvgMax> serializer() {
        return (topic, data) -> {
            try {
                String json = objectMapper.writeValueAsString(data);
                return json.getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("AvgMax serialization failed", e);
            }
        };
    }

    @Override
    public Deserializer<AvgMax> deserializer() {
        return (topic, data) -> {
            try {
                if (data == null || data.length == 0) return null;
                String json = new String(data, StandardCharsets.UTF_8);
                return objectMapper.readValue(json, AvgMax.class);
            } catch (Exception e) {
                throw new RuntimeException("AvgMax deserialization failed", e);
            }
        };
    }
}
