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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqRecoveryConsumer {


    private final ObjectMapper objectMapper;

//    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final ResourceMonitorService resourceMonitorService;
//
//
//    @KafkaListener(
//            topics = KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC + ".DLQ",
//            groupId = "system-resource-metrics-dlq-group",
//            containerFactory = "kafkaListenerContainerSystemMetricsFactory",
//            concurrency = "1"
//    )
//    public void systemResourceMetricsRecover(ConsumerRecord<String, String> record, Acknowledgment ack) {
//        try {
//            if(!isRetryable(record.value())) {
//                KafkaCommonMessage<SystemResourceMetricsMessage> message =
//                        objectMapper.readValue(record.value(), new TypeReference<>() {});
//
//                String partitionKey = message.getBody().serverName();
//                kafkaTemplate.send(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC, partitionKey, record.value());
//
////                ack.acknowledge();
//
//                System.out.println(message.getHeader().getTraceId());
//                System.out.println("중복 아님");
//            } else {
//                // DB 저장 or traceId 기반 중복 방지용 Redis
//                System.out.println("중복 ");
//            }
//
//        } catch (Exception e) {
//            log.error("DLQ 처리 실패", e);
//        }
//    }
//
//    private boolean isRetryable(String value) throws IOException, InterruptedException {
//        KafkaCommonMessage<SystemResourceMetricsMessage> message =
//                objectMapper.readValue(value, new TypeReference<>() {});
//
//        String traceId = message.getHeader().getTraceId();
//
//        return resourceMonitorService.existsByTraceId(traceId);
//    }



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
