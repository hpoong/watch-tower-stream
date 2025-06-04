package com.hopoong.audit.adapter.kafka.resourcemetric.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KTable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;
import com.fasterxml.jackson.core.type.TypeReference;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class SystemMetricsErrorConsumer {

    private final ObjectMapper objectMapper;

    @Bean
    public KTable<String, KafkaCommonMessage<SystemResourceMetricsMessage>> originalTable(
            StreamsBuilder builder,
            Serde<KafkaCommonMessage<SystemResourceMetricsMessage>> originalSerde
    ) {
        KTable<String, String> originalRawTable = builder.table(
                KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String())
        );

        // mapValues() 로 "String → Java 객체 변환" + 변환 후 로그 출력
        KTable<String, KafkaCommonMessage<SystemResourceMetricsMessage>> originalTable = originalRawTable.mapValues(value -> {
            try {
//                return objectMapper.readValue(value, new TypeReference<>() {});

                KafkaCommonMessage<SystemResourceMetricsMessage> message =
                        objectMapper.readValue(value, new TypeReference<>() {});

                String traceId = message.getHeader().getTraceId();
                System.out.println(":::::::::::::::::: >>> " + traceId);

                return null;
            } catch (Exception e) {
                log.error("Failed to parse ORIGINAL message: {}", value, e);
                return null;
            }
        });

//        originalRawTable.mapValues(value -> {
//                    System.out.println(value);
//                    return null;
//                });

        originalTable.toStream().peek((k, v) -> log.info("KTable STATE (after mapValues) - Key: {}, Value: {}", k, v));

//        return originalTable;

        return null;
    }

    @Bean
    public Serde<KafkaCommonMessage<SystemResourceMetricsMessage>> originalSerde() {
        JsonSerde<KafkaCommonMessage<SystemResourceMetricsMessage>> serde =
                new JsonSerde<>(new TypeReference<KafkaCommonMessage<SystemResourceMetricsMessage>>() {}, objectMapper);

        serde.deserializer().addTrustedPackages("*");

        return serde;
    }


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

