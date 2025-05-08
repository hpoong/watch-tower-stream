package com.hopoong.audit.config.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Configuration
@EnableKafkaStreams
@RequiredArgsConstructor
public class KafkaStreamsConfig {

    private final ObjectMapper objectMapper;

    private final ResourceMonitorService resourceMonitorService;


    @Bean
    public KStream<String, String> dlqDelayStream(StreamsBuilder builder) {
//        KStream<String, String> dlqStream = builder.stream("system-resource-metrics.DLQ");
//
//        dlqStream
//            .transform(() -> new DelayedForwarder(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC, Duration.ofSeconds(5)))
//            .to(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC);
//
//        .transform()은 DLQ 메시지에 딜레이를 적용하는 처리 단계,
//        .to(...)는 딜레이가 끝난 메시지를 보낼 정상 토픽을 지정하는 단계
//
//        return dlqStream;

        KStream<String, String> dlqStream = builder.stream("system-resource-metrics.DLQ");

        KStream<String, String>[] branches = dlqStream.branch(
                (key, value) -> {
                    try {
                        return !isRetryable(value);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, // 재시도 가능 (중복 아님) → 정상 토픽으로 보냄
                (key, value) -> true                 // 중복된 경우 → DB 저장 or 무시
        );

        // 정상 메시지만 딜레이 후 전송
        branches[0]
                .peek((key, value) -> log.info("DLQ 재처리 대상: {}", key))
                .transform(() -> new DelayedForwarder(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC, Duration.ofSeconds(5)))
                .to(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC);

        // 중복된 메시지는 DB 저장 또는 로그 출력
        branches[1]
                .peek((key, value) -> {
                    try {
                        log.warn("중복 메시지 감지 → DB 보관 대상: {}", key);
                    } catch (Exception e) {
                        log.error("중복 메시지 DB 저장 실패", e);
                    }
                });

        return dlqStream;
    }

    private boolean isRetryable(String value) throws IOException {
        KafkaCommonMessage<SystemResourceMetricsMessage> message =
                objectMapper.readValue(value, new TypeReference<>() {});

        String traceId = message.getHeader().getTraceId();

        return resourceMonitorService.existsByTraceId(traceId);
    }


}
