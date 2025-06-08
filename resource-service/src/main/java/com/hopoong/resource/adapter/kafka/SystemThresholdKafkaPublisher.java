package com.hopoong.resource.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.message.resourcemonitor.SystemThresholdMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import com.hopoong.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;


@Component
@RequiredArgsConstructor
public class SystemThresholdKafkaPublisher implements Consumer<SystemThresholdMessage> {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public void accept(SystemThresholdMessage message) {

        KafkaCommonMessage<?> kafkaMessage = KafkaCommonMessage.builder()
                .header(
                        KafkaCommonMessage.Header.builder()
                            .topic(KafkaTopicManager.SYSTEM_THRESHOLD_TOPIC)
                            .type("SYSTEM_METRICS")
                            .traceId(UUID.randomUUID().toString())
                            .timestamp(RandomUtil.getCurrentTime())
                            .build()
                )
                .body(message)
                .build();

        String payload = objectMapper.writeValueAsString(kafkaMessage);
        String partitionKey = message.serverName() + ":" + message.resourceName();
        kafkaTemplate.send(KafkaTopicManager.SYSTEM_THRESHOLD_TOPIC, partitionKey, payload);
    }
}
