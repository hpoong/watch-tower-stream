package com.hopoong.audit.adapter.kafka;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.common.kafka.DelayedForwarder;
import com.hopoong.audit.usecase.resourcemonitor.ResourceMonitorService;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqRecoveryConsumer {


    private final ObjectMapper objectMapper;

    private final ResourceMonitorService resourceMonitorService;

    @Bean
    public KStream<String, String> dlqDelayStream(StreamsBuilder builder) {

        KStream<String, String> dlqStream = builder.stream(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC + ".DLQ");

        KStream<String, String>[] branches = dlqStream.branch(
                (key, value) -> !isTraceIdInResourceMonitor(value),
                (key, value) -> true
        );

        // 정상 메시지만 딜레이 후 전송
        branches[0]
                .peek((key, value) -> log.info("DLQ 재처리 대상: {}", key))
                .transform(() -> new DelayedForwarder(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC, Duration.ofSeconds(5)))
                .to(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC);

        // 중복된 메시지는 DB 저장 또는 로그 출력
        branches[1]
                .peek((key, value) -> log.warn("중복 메시지 감지 → DB 보관 대상: {}", key));

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
