package com.hopoong.resource.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.message.resourcemonitor.SystemThresholdMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import com.hopoong.core.util.RandomUtil;
import com.hopoong.resource.exception.KafkaPublishException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPublisher<T> implements Consumer<T> {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void accept(T message) {
        Class<?> clazz = message.getClass();
        if (clazz == SystemResourceMetricsMessage.class) {
            publish(message, KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC, "SYSTEM_METRICS");
        } else if (clazz == SystemThresholdMessage.class) {
            publish(message, KafkaTopicManager.SYSTEM_THRESHOLD_TOPIC, "SYSTEM_THRESHOLD");
        } else {
            throw new IllegalArgumentException("지원하지 않는 메시지 타입입니다: " + clazz.getSimpleName());
        }
    }

    public void publish(T message, String topic, String type) {
        try {
            KafkaCommonMessage<T> kafkaMessage = buildKafkaMessage(message, topic, type);
            String payload = objectMapper.writeValueAsString(kafkaMessage);
            String partitionKey = generatePartitionKey(message);
            kafkaTemplate.send(topic, partitionKey, payload);
        } catch (Exception e) {
            throw new KafkaPublishException("Failed to publish message to topic: " + topic, topic, type, e);
        }
    }

    private KafkaCommonMessage<T> buildKafkaMessage(T message, String topic, String type) {
        return KafkaCommonMessage.<T>builder()
                .header(KafkaCommonMessage.Header.builder()
                        .topic(topic)
                        .type(type)
                        .traceId(UUID.randomUUID().toString())
                        .timestamp(RandomUtil.getCurrentTime())
                        .build())
                .body(message)
                .build();
    }

    private String generatePartitionKey(T message) {
        Class<?> clazz = message.getClass();
        if (clazz == SystemResourceMetricsMessage.class) {
            SystemResourceMetricsMessage metricsMessage = (SystemResourceMetricsMessage) message;
            return metricsMessage.serverName() + ":" + metricsMessage.resourceName();
        } else if (clazz == SystemThresholdMessage.class) {
            SystemThresholdMessage thresholdMessage = (SystemThresholdMessage) message;
            return thresholdMessage.serverName() + ":" + thresholdMessage.resourceName();
        } else {
            return UUID.randomUUID().toString();
        }
    }
}