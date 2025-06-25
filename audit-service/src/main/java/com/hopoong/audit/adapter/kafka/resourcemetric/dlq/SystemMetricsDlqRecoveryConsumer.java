package com.hopoong.audit.adapter.kafka.resourcemetric.dlq;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.adapter.kafka.resourcemetric.transformer.DelayedForwarderTransformer;
import com.hopoong.audit.adapter.kafka.resourcemetric.transformer.ErrorForwarderTransformer;
import com.hopoong.audit.usecase.resourcemonitor.ResourceMonitorService;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import com.hopoong.core.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMetricsDlqRecoveryConsumer {


    private final ObjectMapper objectMapper;
    private final ResourceMonitorService resourceMonitorService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Bean
    public KStream<String, String> dlqDelayStream(StreamsBuilder builder) {

        KStream<String, String> dlqStream = builder.stream(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_DLQ_TOPIC);

        KStream<String, String>[] branches = dlqStream.branch(
                (key, value) -> !isTraceIdInResourceMonitor(value),
                (key, value) -> true
        );

        // 정상 메시지만 딜레이 후 전송
        branches[0]
            .peek((key, value) -> LoggerUtil.section(log, "[DLQ 소비] Topic = %s".formatted(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_DLQ_TOPIC)))
            .transform(() -> new DelayedForwarderTransformer(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_REPROCESS_TOPIC, Duration.ofSeconds(5), kafkaTemplate));

        // 중복된 메시지는 ERROR 토픽으로 전송
        branches[1]
            .peek((key, value) -> LoggerUtil.section(log, "[중복 감지] DB 저장 대상 키 = %s".formatted(key)))
            .transform(() -> new ErrorForwarderTransformer(kafkaTemplate, objectMapper, KafkaTopicManager.SYSTEM_RESOURCE_METRICS_ERROR_TOPIC));

        return dlqStream;
    }

    private boolean isTraceIdInResourceMonitor(String value) {
        try {
            Thread.sleep(5000);

            KafkaCommonMessage<SystemResourceMetricsMessage> message =
                    objectMapper.readValue(value, new TypeReference<>() {});

            String traceId = message.getHeader().getTraceId();

            return resourceMonitorService.existsByTraceId(traceId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
