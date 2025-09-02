package com.hopoong.audit.adapter.kafka.user.action.stream;

import com.carroti.um.FeatureCount5mRecord;
import com.hopoong.avro.message.UserActionEventMessage;
import com.hopoong.core.topic.KafkaStoreManager;
import com.hopoong.core.topic.KafkaTopicManager;
import com.hopoong.core.util.TimeUtil;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class FeatureCountTopology {

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean
    public SpecificAvroSerde<UserActionEventMessage> userActionSerde() {
        return specificAvroSerde();
    }

    @Bean
    public SpecificAvroSerde<FeatureCount5mRecord> featureCountSerde() {
        return specificAvroSerde();
    }

    @Bean
    public <T extends SpecificRecord> SpecificAvroSerde<T> specificAvroSerde() {
        SpecificAvroSerde<T> serde = new SpecificAvroSerde<>();
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        serde.configure(cfg, false);
        return serde;
    }

    @Bean
    public KStream<String, FeatureCount5mRecord> featureCountStream(
            @Qualifier("avroStreamsBuilderFactoryBean") StreamsBuilder builder,
            SpecificAvroSerde<UserActionEventMessage> userActionSerde,
            SpecificAvroSerde<FeatureCount5mRecord> featureCountSerde
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


//        byTenantFeature.peek((key, value) -> {
//            System.out.println("input test ========================");
//            System.out.println(key);
//            System.out.println(value);
//        });

        // 윈도우 집계: 5분 Tumbling, grace 1분
        Duration windowSize = Duration.ofMinutes(5);
        Duration grace = Duration.ofMinutes(1);

        KTable<Windowed<String>, Long> counts = byTenantFeature
                .groupByKey(Grouped.with(stringSerde, userActionSerde))
                .windowedBy(TimeWindows.ofSizeAndGrace(windowSize, grace))
                .count(Materialized.as(KafkaStoreManager.USER_SERVICE_FEATURE_COUNTS_STORE));

        KStream<String, FeatureCount5mRecord> out = counts.toStream()
                .map((windowedKey, count) -> {
                    String k = windowedKey.key();           // tenant|feature
                    int sep = k.indexOf('|');
                    String tenantId = k.substring(0, sep);
                    String feature  = k.substring(sep + 1);
                    long windowStart = windowedKey.window().start();
                    long windowEnd   = windowedKey.window().end();

                    FeatureCount5mRecord rec = FeatureCount5mRecord.newBuilder()
                            .setTenantId(tenantId)
                            .setFeature(feature)
                            .setWindowStart(Instant.ofEpochMilli(windowStart))
                            .setWindowEnd(Instant.ofEpochMilli(windowEnd))
                            .setCount(count)
                            .setEmittedAt(TimeUtil.nowInstantUtc())
                            .build();

                    // 파티션 키: tenantId|feature|windowStart
                    String newKey = tenantId + "|" + feature + "|" + windowStart;
                    return KeyValue.pair(newKey, rec);
                });

//        out.peek((k, v) -> {
//            System.out.println("out test ================");
//            System.out.println(k);
//            System.out.println(v);
//        });

        out.to(
                KafkaTopicManager.USER_FEATURE_COUNT_5M,
                Produced.with(stringSerde, featureCountSerde)
        );

        return out;
    }
}