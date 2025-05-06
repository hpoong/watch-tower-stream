package com.hopoong.audit.adapter.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.common.exception.KafkaProcessingException;
import com.hopoong.audit.usecase.resourcemonitor.ResourceMonitorService;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMetricsKafkaConsumer {

    private final ObjectMapper objectMapper;

    private final Map<String, List<SystemResourceMetricsMessage>> serverMessageMap = new ConcurrentHashMap<>();

    private final ResourceMonitorService resourceMonitorService;

    @KafkaListener(
            topics = KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC,
            groupId = "system-resource-metrics-group",
            containerFactory = "kafkaListenerContainerSystemMetricsFactory",
            concurrency = "1"
    )
    public void consumeSystemResourceMetrics(ConsumerRecord<String, String> record, Acknowledgment ack) throws IOException {
        try {
            KafkaCommonMessage<SystemResourceMetricsMessage> message =
                    objectMapper.readValue(record.value(), new TypeReference<>() {});

            SystemResourceMetricsMessage body = message.getBody();
            KafkaCommonMessage.Header header = message.getHeader();
            String server = body.serverName();

            resourceMonitorService.insertSystemResourceMetrics(message);

            // 출력용
            serverMessageMap
                    .computeIfAbsent(server, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(body);

            if (serverMessageMap.get(server).size() == 3) {
                List<SystemResourceMetricsMessage> sortedList = serverMessageMap.get(server).stream()
                        .sorted(Comparator.comparing(SystemResourceMetricsMessage::resourceName))
                        .toList();

                log.info("[SEQ-CHECK] {}:", server);
                for (SystemResourceMetricsMessage m : sortedList) {
                    log.info("  → [{}] {}% ({}) @ {}",
                            m.resourceName(),
                            String.format("%.3f", m.usagePercent()),
                            m.alertLevel(),
                            header.getTimestamp()
                    );
                }
                serverMessageMap.get(server).clear();

                // 강제 에러 처리
                throw new KafkaProcessingException(record.topic(), record.partition(), record.offset(), header.getTraceId());
            }

            ack.acknowledge();

        } catch (Exception e) {
            KafkaCommonMessage.Header header = extractHeaderSafely(record);
            log.error("[CONSUMER ERROR] Failed to process message", e);
            log.error("  ↳ partition={}, offset={}, traceId={}, topic={}",
                    record.partition(),
                    record.offset(),
                    Optional.ofNullable(header)
                            .map(KafkaCommonMessage.Header::getTraceId)
                            .orElse("UNKNOWN"),
                    Optional.ofNullable(header)
                            .map(KafkaCommonMessage.Header::getTopic)
                            .orElse("UNKNOWN")
            );

            throw e;
        }
    }

    private KafkaCommonMessage.Header extractHeaderSafely(ConsumerRecord<String, String> record) {
        try {
            KafkaCommonMessage<SystemResourceMetricsMessage> message =
                    objectMapper.readValue(record.value(), new TypeReference<>() {});
            return message.getHeader();
        } catch (JsonProcessingException e) {
            return null;
        }
    }

}
