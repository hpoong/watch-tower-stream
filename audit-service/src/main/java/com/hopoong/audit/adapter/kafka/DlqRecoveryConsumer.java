package com.hopoong.audit.adapter.kafka;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.usecase.resourcemonitor.ResourceMonitorService;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqRecoveryConsumer {


//    private final ObjectMapper objectMapper;
//
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    private final ResourceMonitorService resourceMonitorService;
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


}
