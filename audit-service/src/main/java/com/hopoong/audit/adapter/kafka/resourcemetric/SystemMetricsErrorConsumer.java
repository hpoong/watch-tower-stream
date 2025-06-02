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

//    private final ObjectMapper objectMapper;
//
//    @Bean
//    public KStream<String, KafkaCommonMessage<SystemResourceMetricsErrorMessage>> systemMetricsErrorStream(StreamsBuilder builder) {
//
//        // 원본 KTable 구성
//        KTable<String, KafkaCommonMessage<SystemResourceMetricsMessage>> originalTable = builder.table(
//                KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC,
//                Consumed.with(Serdes.String(), originalSerde())
//        );
//
//        // ERROR KStream 구성
//        KStream<String, KafkaCommonMessage<SystemResourceMetricsErrorMessage>> errorStream = builder.stream(
//                KafkaTopicManager.SYSTEM_RESOURCE_METRICS_ERROR_TOPIC,
//                Consumed.with(Serdes.String(), errorSerde())
//        );
//
//        // ERROR Stream 로그 출력
//        errorStream.peek((k, v) -> log.info("ERROR STREAM - Key: {}, Value: {}", k, v));
//
//        // KStream + KTable JOIN (EnrichedErrorMessage 생성)
//        KStream<String, EnrichedErrorMessage> enrichedErrorStream = errorStream.join(
//                originalTable,
//                (error, original) -> EnrichedErrorMessage.builder()
//                        .original(original)
//                        .error(error)
//                        .build()
//        );
//
//        // ENRICHED Stream 로그 출력
//        enrichedErrorStream.peek((k, v) -> log.info("ENRICHED STREAM - Key: {}, Value: {}", k, v));
//
//        // ENRICHED Topic 으로 전송
//        enrichedErrorStream.to(
//                KafkaTopicManager.SYSTEM_RESOURCE_METRICS_ERROR_ENRICHED_TOPIC,
//                Produced.with(Serdes.String(), enrichedSerde())
//        );
//
//        return errorStream;
//    }
//
//    // 원본 Serde
//    @Bean
//    public Serde<KafkaCommonMessage<SystemResourceMetricsMessage>> originalSerde() {
//        JsonSerializer<KafkaCommonMessage<SystemResourceMetricsMessage>> serializer = new JsonSerializer<>(objectMapper);
//        JsonDeserializer<KafkaCommonMessage<SystemResourceMetricsMessage>> deserializer =
//                new JsonDeserializer<>(KafkaCommonMessage.class, objectMapper);
//        return Serdes.serdeFrom(serializer, deserializer);
//    }
//
//    // ERROR Serde
//    @Bean
//    public Serde<KafkaCommonMessage<SystemResourceMetricsErrorMessage>> errorSerde() {
//        JsonSerializer<KafkaCommonMessage<SystemResourceMetricsErrorMessage>> serializer = new JsonSerializer<>(objectMapper);
//        JsonDeserializer<KafkaCommonMessage<SystemResourceMetricsErrorMessage>> deserializer =
//                new JsonDeserializer<>(KafkaCommonMessage.class, objectMapper);
//        return Serdes.serdeFrom(serializer, deserializer);
//    }
//
//    // ENRICHED Serde
//    @Bean
//    public Serde<EnrichedErrorMessage> enrichedSerde() {
//        JsonSerializer<EnrichedErrorMessage> serializer = new JsonSerializer<>(objectMapper);
//        JsonDeserializer<EnrichedErrorMessage> deserializer = new JsonDeserializer<>(EnrichedErrorMessage.class, objectMapper);
//        return Serdes.serdeFrom(serializer, deserializer);
//    }
//
//    // JOIN 결과 클래스 (JOIN 용도 전용)
//    @Builder
//    public record EnrichedErrorMessage(
//            KafkaCommonMessage<SystemResourceMetricsMessage> original,
//            KafkaCommonMessage<SystemResourceMetricsErrorMessage> error
//    ) {}
}

