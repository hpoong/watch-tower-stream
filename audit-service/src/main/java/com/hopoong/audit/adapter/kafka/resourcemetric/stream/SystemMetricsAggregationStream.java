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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMetricsAggregationStream {

    private final ObjectMapper objectMapper;

    @Bean
    public KStream<String, String> cpu5minAvgMaxStream(StreamsBuilder builder) {
        return build5minAvgMaxStream(builder, "CPU");
    }

    @Bean
    public KStream<String, String> disk5minAvgMaxStream(StreamsBuilder builder) {
        return build5minAvgMaxStream(builder, "DISK");
    }

    @Bean
    public KStream<String, String> memory5minAvgMaxStream(StreamsBuilder builder) {
        return build5minAvgMaxStream(builder, "Memory");
    }

    /*
     * 리소스 사용량 5분 평균 값 및 min, max 값 조회
     */
    public KStream<String, String> build5minAvgMaxStream(StreamsBuilder builder, String type) {

        // system-resource-metrics : KStream
        GenericJsonSerde<KafkaCommonMessage<SystemResourceMetricsMessage>> resourceSerde =
                new GenericJsonSerde<>(objectMapper, new TypeReference<KafkaCommonMessage<SystemResourceMetricsMessage>>() {});

        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> resourceMetricsStream = builder.stream(
                KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC,
                Consumed.with(Serdes.String(), resourceSerde)
        );

        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> stream =
            resourceMetricsStream.filter((key, value) ->
                    value.getBody().resourceName().equalsIgnoreCase(type)
            );

        stream.foreach((key, value) -> {
            LoggerUtil.section(log, "[data stream] key = %s, serverName = %s, value = %s".formatted(key, value.getBody().serverName(), value.getBody().usagePercent()));
        });

        TimeWindows oneMinute = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1));
        TimeWindows fiveMinutes = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5));
        TimeWindows tenMinutes = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(10));

        // 5분 평균
        KTable<Windowed<String>, AvgMax> avgMaxOneMin = stream
            .groupByKey()
            .windowedBy(fiveMinutes)
            .aggregate(
                    AvgMax::new,
                    (key, value, aggregate) -> aggregate.add(value.getBody().usagePercent()),
                    Materialized.with(Serdes.String(), new AvgMaxSerde(objectMapper))
            )
            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()));


        avgMaxOneMin.toStream().foreach((windowedKey, value) -> {

            long startEpoch = windowedKey.window().start();
            long endEpoch = windowedKey.window().end();

            String startTime = Instant.ofEpochMilli(startEpoch)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String endTime = Instant.ofEpochMilli(endEpoch)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            LoggerUtil.section(log, """
                [%s][%s] 5분 평균 = %.2f, 최대 = %.2f
                윈도우 시작: %s
                윈도우 종료: %s
                """.formatted(
                            windowedKey.key(), type,
                            value.avg(), value.max(),
                            startTime, endTime
                    ));
        });

        return null;
    }


}
