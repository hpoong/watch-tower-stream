package com.hopoong.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.avro.message.UserActionEventMessage;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import java.io.ByteArrayOutputStream;

public class AvroJsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AvroJsonUtil() {}

    public static <T extends org.apache.avro.specific.SpecificRecord> JsonNode toJsonNode(T avro) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            DatumWriter<T> writer = new SpecificDatumWriter<>((Class<T>) avro.getClass());
            Encoder encoder = EncoderFactory.get().jsonEncoder(avro.getSchema(), out);
            writer.write(avro, encoder);
            encoder.flush();
            out.flush();
            return objectMapper.readTree(out.toString());
        }
    }


    public static SpecificRecord toAvroRecord(String schemaAlias, JsonNode payload) throws JsonProcessingException {
        switch (schemaAlias) {
            case "UserActionEventMessage":
                return objectMapper.treeToValue(
                        payload,
                        UserActionEventMessage.class);

            default:
                throw new IllegalArgumentException("지원하지 않는 스키마: " + schemaAlias);
        }
    }


}
