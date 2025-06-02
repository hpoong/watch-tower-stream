package com.hopoong.audit.adapter.kafka.resourcemetric;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsErrorMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMetricsErrorConsumer {

    private final ObjectMapper objectMapper;

//    @Bean
//    public Serde<KafkaCommonMessage<SystemResourceMetricsMessage>> originalSerde(ObjectMapper objectMapper) {
//        JsonSerializer<KafkaCommonMessage<SystemResourceMetricsMessage>> serializer = new JsonSerializer<>(objectMapper);
//        JsonDeserializer<KafkaCommonMessage<SystemResourceMetricsMessage>> deserializer =
//                new JsonDeserializer<>(KafkaCommonMessage.class, objectMapper);
//        return Serdes.serdeFrom(serializer, deserializer);
//    }
//
//
//    @Bean
//    public Serde<KafkaCommonMessage<SystemResourceMetricsErrorMessage>> errorSerde(ObjectMapper objectMapper) {
//        JsonSerializer<KafkaCommonMessage<SystemResourceMetricsErrorMessage>> serializer = new JsonSerializer<>(objectMapper);
//        JsonDeserializer<KafkaCommonMessage<SystemResourceMetricsErrorMessage>> deserializer =
//                new JsonDeserializer<>(KafkaCommonMessage.class, objectMapper);
//        return Serdes.serdeFrom(serializer, deserializer);
//    }
//
//    @Bean
//    public Serde<EnrichedErrorMessage> enrichedSerde(ObjectMapper objectMapper) {
//        JsonSerializer<EnrichedErrorMessage> serializer = new JsonSerializer<>(objectMapper);
//        JsonDeserializer<EnrichedErrorMessage> deserializer = new JsonDeserializer<>(EnrichedErrorMessage.class, objectMapper);
//        return Serdes.serdeFrom(serializer, deserializer);
//    }
//
//    @Builder
//    public record EnrichedErrorMessage(
//            KafkaCommonMessage<SystemResourceMetricsMessage> original,
//            KafkaCommonMessage<SystemResourceMetricsErrorMessage> error
//    ) {}
//
//
//    @Bean
//    public KStream<String, KafkaCommonMessage<SystemResourceMetricsErrorMessage>> systemMetricsErrorStream(StreamsBuilder builder, ObjectMapper objectMapper) {
//
//        // 원본 KTable
//        KTable<String, KafkaCommonMessage<SystemResourceMetricsMessage>> originalTable = builder
//                .table(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC, Consumed.with(Serdes.String(), originalSerde(objectMapper)));
//
//        // ERROR Stream
//        KStream<String, KafkaCommonMessage<SystemResourceMetricsErrorMessage>> errorStream = builder
//                .stream(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_ERROR_TOPIC, Consumed.with(Serdes.String(), errorSerde(objectMapper)));
//
//        // Logging
//        errorStream.peek((k, v) -> System.out.println("ERROR : " + v));
//
//        // JOIN
//        KStream<String, EnrichedErrorMessage> enrichedErrorStream = errorStream.join(
//                originalTable,
//                (error, original) -> EnrichedErrorMessage.builder()
//                        .original(original)
//                        .error(error)
//                        .build()
//        );
//
//        // Logging enriched
//        enrichedErrorStream.peek((k, v) -> System.out.println("ENRICHED : " + v));
//
//        // Output to ENRICHED Topic
//        enrichedErrorStream.to(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_ERROR_ENRICHED_TOPIC,
//                Produced.with(Serdes.String(), enrichedSerde(objectMapper)));
//
//        return errorStream;
//    }

}

