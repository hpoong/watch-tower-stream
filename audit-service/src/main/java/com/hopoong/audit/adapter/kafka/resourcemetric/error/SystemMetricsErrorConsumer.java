package com.hopoong.audit.adapter.kafka.resourcemetric.error;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.common.serde.GenericJsonSerde;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsErrorMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.topic.KafkaStoreManager;
import com.hopoong.core.topic.KafkaTopicManager;
import com.hopoong.core.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class SystemMetricsErrorConsumer {


    /*
     * system-resource-metrics ERROR 집계 처리
     */
    @Bean
    public KTable<String, KafkaCommonMessage<SystemResourceMetricsMessage>> systemMetricsErrorStream(
            StreamsBuilder builder,
            ObjectMapper objectMapper
    ) {

        // KTable - ERROR
        GenericJsonSerde<KafkaCommonMessage<SystemResourceMetricsErrorMessage>> messageSerde =
                new GenericJsonSerde<>(objectMapper, new TypeReference<KafkaCommonMessage<SystemResourceMetricsErrorMessage>>() {});

        KTable<String, KafkaCommonMessage<SystemResourceMetricsErrorMessage>> errorKTable = builder.table(
                KafkaTopicManager.SYSTEM_RESOURCE_METRICS_ERROR_TOPIC,
                Consumed.with(Serdes.String(), messageSerde)
        );

        // 에러 유형별 카운트 테이블 생성
        KTable<String, Long> errorTypeCountTable = errorKTable.toStream()
            .map(
                (key, value) -> {
                    String serverName = value.getBody().originalMessage().serverName();
                    String errorType = value.getBody().errorType();
                    String newKey = serverName + "|" + errorType;
                    return KeyValue.pair(newKey, ""); // count 목적이기에 빈값 처리
                }
            )
            .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
            .count(Materialized.as(KafkaStoreManager.SYSTEM_RESOURCE_METRICS_ERROR_COUNT_STORE));


        errorTypeCountTable
            .toStream()
            .foreach((key, value) -> {
                LoggerUtil.section(log, "[Error 집계] key = %s, count = %s".formatted(key, value));
            });

        return null;
    }
}

