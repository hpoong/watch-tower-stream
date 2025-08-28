package com.hopoong.audit.adapter.kafka.user.action.stream;

import com.hopoong.avro.message.UserActionEventMessage;
import com.hopoong.core.topic.KafkaStoreManager;
import com.hopoong.core.topic.KafkaTopicManager;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class FeatureCountTopology {

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean
    public SpecificAvroSerde<UserActionEventMessage> userActionSerde() {
        SpecificAvroSerde<UserActionEventMessage> serde = new SpecificAvroSerde<>();
        Map<String, String> cfg = new HashMap<>();
        cfg.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        // value serde
        serde.configure(cfg, false);
        return serde;
    }

    @Bean
    public KStream<String, String> featureCountStream(
            @Qualifier("avroStreamsBuilderFactoryBean") StreamsBuilder builder,
            SpecificAvroSerde<UserActionEventMessage> userActionSerde
    ) {
        final Serde<String> stringSerde = Serdes.String();

        // 입력 스트림
        KStream<String, UserActionEventMessage> input = builder
                .stream(KafkaTopicManager.USER_ACTION_EVENTS, Consumed.with(stringSerde, userActionSerde));

        // 정합성 필터링
        KStream<String, UserActionEventMessage> valid = input
            .filter((k, v) -> {
                var body = v.getBody();
                return body.getTenantId() != null && !body.getTenantId().isEmpty()
                        && body.getFeature() != null && !body.getFeature().isEmpty()
                        && body.getOccurredAt() != null;
            });

        // 키 재설정: tenantId|feature 로 파티션 일관성 유지
        KStream<String, UserActionEventMessage> byTenantFeature = valid
            .selectKey((k, v) -> {
                var body = v.getBody();
                return body.getTenantId() + "|" + body.getFeature();
            });

        // 윈도우 집계: 5분 Tumbling, grace 1분
        Duration windowSize = Duration.ofMinutes(5);
        Duration grace = Duration.ofMinutes(1);

//        TimeWindows windows = TimeWindows
//                .ofSizeWithNoGrace(windowSize)
//                .advanceBy(windowSize);

        KTable<Windowed<String>, Long> counts = byTenantFeature
                .groupByKey(Grouped.with(stringSerde, userActionSerde))
                .windowedBy(TimeWindows.ofSizeAndGrace(windowSize, grace))
                .count(Materialized.as(KafkaStoreManager.USER_SERVICE_FEATURE_COUNTS_STORE));

        // 윈도우 종료 시점에만 결과를 보냄
        KTable<Windowed<String>, Long> finalCounts = counts
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()));

        finalCounts.toStream().peek((k, v) -> {
            System.out.println("========================");
            System.out.println(k);
            System.out.println(v);
            System.out.println("========================");
        });

        return null;
    }
}