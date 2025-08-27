package com.hopoong.audit.adapter.kafka.resourcemetric.stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.message.resourcemonitor.SystemThresholdMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import com.hopoong.core.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class SystemMetricsAlertStream extends AbstractMetricsStream {

    public SystemMetricsAlertStream(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /*
     * 리소스 사용량 임계치 초과시 알람처리
     */
    @Bean
    public KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> systemMetricsThresholdAlertStream(
            @Qualifier("defaultKafkaStreamsBuilder") StreamsBuilder builder
    ) {
        // 스트림 생성 및 테이블 생성
        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> metricsStream = createMetricsStream(builder);
        KTable<String, KafkaCommonMessage<SystemThresholdMessage>> thresholdTable = createThresholdTable(builder);

        // JOIN 수행
        KStream<String, Double> alertStream = filterExceededThreshold(metricsStream, thresholdTable);

        // 저장 및 로깅
        alertStream.foreach((key, value) -> {
            LoggerUtil.section(log, """
            [임계치 초과]
            key = %s
            value = %s
            """.formatted(key, value));
        });

        return metricsStream;
    }

    // stream 생성
    private KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> createMetricsStream(StreamsBuilder builder) {
        return createResourceMetricsStream(
                builder,
                KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC,
                createSerde(new TypeReference<KafkaCommonMessage<SystemResourceMetricsMessage>>() {})
        );
    }

    // table 생성
    private KTable<String, KafkaCommonMessage<SystemThresholdMessage>> createThresholdTable(StreamsBuilder builder) {
        return parseKTable(
                builder,
                KafkaTopicManager.SYSTEM_THRESHOLD_TOPIC,
                new TypeReference<KafkaCommonMessage<SystemThresholdMessage>>() {}
        );
    }

    // join 처리 
    private KStream<String, Double> filterExceededThreshold(
            KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> stream,
            KTable<String, KafkaCommonMessage<SystemThresholdMessage>> thresholdTable
    ) {
        return stream
                .join(thresholdTable, (resource, threshold) -> {
                    double usage = resource.getBody().usagePercent();
                    double limit = threshold.getBody().thresholdValue();
                    return usage > limit ? usage : null;
                })
                .filter((key, value) -> value != null);
    }

}
