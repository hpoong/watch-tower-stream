package com.hopoong.audit.adapter.kafka.resourcemetric.stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.adapter.kafka.resourcemetric.model.AvgMax;
import com.hopoong.audit.common.serde.AvgMaxSerde;
import com.hopoong.audit.common.serde.GenericJsonSerde;
import com.hopoong.audit.usecase.resourcemonitor.ResourceMonitorService;
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
import org.springframework.util.function.ThrowingConsumer;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component

public class SystemMetricsAggregationStream extends AbstractMetricsStream {

    private final ResourceMonitorService resourceMonitorService;

    public SystemMetricsAggregationStream(ObjectMapper objectMapper, ResourceMonitorService resourceMonitorService) {
        super(objectMapper);
        this.resourceMonitorService = resourceMonitorService;
    }

    @Bean
    public List<KStream<String, String>> avgMax5MinStreams(StreamsBuilder builder) {
        List<String> resourceTypes = List.of("CPU", "DISK", "Memory");

        return resourceTypes.stream()
                .map(type -> buildAvgMaxStream(
                        builder,
                        type,
                        TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)),
                        resourceMonitorService::saveSystemResourceMetrics5Min)
                )
                .collect(Collectors.toList());
    }

    /*
     * 리소스 사용량 N분 평균 값 및 min, max 값 조회
     */
    public KStream<String, String> buildAvgMaxStream(StreamsBuilder builder, String type, TimeWindows timeWindows,  ThrowingConsumer<AvgMax> persistFunction) {

        GenericJsonSerde<KafkaCommonMessage<SystemResourceMetricsMessage>> serde
                    = createSerde(new TypeReference<KafkaCommonMessage<SystemResourceMetricsMessage>>() {});

        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> stream
                = createResourceMetricsStream(builder, KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC, serde);

        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> filteredStream
                = filterByResourceType(stream, type);

        // N분 평균 집계
        KTable<Windowed<String>, AvgMax> avgMaxOneMin = filteredStream
            .groupByKey()
            .windowedBy(timeWindows)
            .aggregate(
                    AvgMax::new,
                    (key, value, aggregate) -> aggregate.add(value.getBody().usagePercent(), LocalDateTime.now()),
                    Materialized.with(Serdes.String(), new AvgMaxSerde(objectMapper))
            )
            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()));


        // N분 데이터 저장.
        avgMaxOneMin.toStream().foreach((windowedKey, value) -> {
            long startEpoch = windowedKey.window().start();
            long endEpoch = windowedKey.window().end();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            LocalDateTime startDateTime = Instant.ofEpochMilli(startEpoch)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            LocalDateTime endDateTime = Instant.ofEpochMilli(endEpoch)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            String[] keyParts = windowedKey.key().split(":");
            value.setTimestamp(endDateTime);
            value.setServerName(keyParts[0]);
            value.setResourceName(keyParts[1]);

            persistFunction.accept(value);

            LoggerUtil.section(log, """
                [리소스 사용량]
                %s
                5분 평균 = %.2f
                최대 = %.2f
                윈도우 시작: %s
                윈도우 종료: %s
                """.formatted(
                    windowedKey.key(),
                    value.avg(), value.max(),
                    startDateTime.format(formatter),
                    endDateTime.format(formatter)
            ));
        });

        return null;
    }


}
