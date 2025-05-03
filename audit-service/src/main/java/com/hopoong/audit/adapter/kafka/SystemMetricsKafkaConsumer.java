package com.hopoong.audit.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMetricsKafkaConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC,
            groupId = "is-not-batch",
            containerFactory = "kafkaBatchListenerContainerFactory",
            concurrency = "1"
    )
    public void isNotBatch(List<ConsumerRecord<String, String>> messages) {

    }


}
