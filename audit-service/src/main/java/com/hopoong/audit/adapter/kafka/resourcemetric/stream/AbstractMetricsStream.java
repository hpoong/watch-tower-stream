package com.hopoong.audit.adapter.kafka.resourcemetric.stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.common.serde.GenericJsonSerde;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;

public abstract class AbstractMetricsStream {

    protected final ObjectMapper objectMapper;

    protected AbstractMetricsStream(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // 공통 Serde 생성
    protected <T> GenericJsonSerde<KafkaCommonMessage<T>> createSerde(TypeReference<KafkaCommonMessage<T>> typeRef) {
        return new GenericJsonSerde<>(objectMapper, typeRef);
    }

    // 공통 KStream 생성
    protected <T> KStream<String, KafkaCommonMessage<T>> createResourceMetricsStream(
            StreamsBuilder builder, String topic, GenericJsonSerde<KafkaCommonMessage<T>> serde) {
        return builder.stream(topic, Consumed.with(Serdes.String(), serde));
    }

    // 공통 필터
    protected <T> KStream<String, KafkaCommonMessage<T>> filterByResourceType(
            KStream<String, KafkaCommonMessage<T>> stream, String type) {
        return stream.filter((key, value) -> ((SystemResourceMetricsMessage) value.getBody()).resourceName().equalsIgnoreCase(type));
    }

    // KTable<String, String>을 KTable<String, KafkaCommonMessage<T>>로 변환
    protected <T> KTable<String, KafkaCommonMessage<T>> parseKTable(
            StreamsBuilder builder,
            String topic,
            TypeReference<KafkaCommonMessage<T>> typeRef
    ) {
        KTable<String, String> rawTable = builder.table(
                topic,
                Consumed.with(Serdes.String(), Serdes.String())
        );

        return rawTable.mapValues(value -> {
            try {
                String jsonString = objectMapper.readValue(value, String.class);
                return objectMapper.readValue(jsonString, typeRef);
            } catch (Exception e) {
                return null;
            }
        });
    }


}