package com.hopoong.audit.adapter.kafka.resourcemetric.stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.adapter.kafka.resourcemetric.model.AvgMax;
import com.hopoong.audit.common.serde.AvgMaxSerde;
import com.hopoong.audit.common.serde.GenericJsonSerde;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import com.hopoong.core.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMetricsAggregationStream {

    private final ObjectMapper objectMapper;

    @Bean
    public KStream<String, String> systemMetricsThresholdAvgMaxOneMinStream(StreamsBuilder builder) {

        // system-resource-metrics : KStream
        GenericJsonSerde<KafkaCommonMessage<SystemResourceMetricsMessage>> resourceSerde =
                new GenericJsonSerde<>(objectMapper, new TypeReference<KafkaCommonMessage<SystemResourceMetricsMessage>>() {});

        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> resourceMetricsStream = builder.stream(
                KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC,
                Consumed.with(Serdes.String(), resourceSerde)
        );

        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> cpuStream =
                resourceMetricsStream.filter((key, value) ->
                        value.getBody().resourceName().equalsIgnoreCase("CPU")
                );


        TimeWindows oneMinute = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1));
        TimeWindows fiveMinutes = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5));
        TimeWindows tenMinutes = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(10));

        KTable<Windowed<String>, AvgMax> avgMaxOneMin = cpuStream
                .groupByKey()
                .windowedBy(fiveMinutes)
                .aggregate(
                        AvgMax::new,
                        (key, value, aggregate) -> aggregate.add(value.getBody().usagePercent()),
                        Materialized.with(Serdes.String(), new AvgMaxSerde(objectMapper))
                );

        avgMaxOneMin.toStream().foreach((windowedKey, value) -> {
            LoggerUtil.section(log, "[5분] CPU 평균 = %.2f, 최대 = %.2f".formatted(value.avg(), value.max()));
        });

        return null;
    }


}
