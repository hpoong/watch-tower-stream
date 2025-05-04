package com.hopoong.audit.adapter.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMetricsKafkaConsumer {

    private final ObjectMapper objectMapper;

    private final Map<String, List<SystemResourceMetricsMessage>> serverMessageMap = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC,
            groupId = "system-resource-metrics-group",
            containerFactory = "kafkaListenerContainerSystemMetricsFactory",
            concurrency = "1"
    )
    public void consumeSystemResourceMetrics(ConsumerRecord<String, String> record) throws JsonProcessingException {
        KafkaCommonMessage<SystemResourceMetricsMessage> message =
                objectMapper.readValue(record.value(), new TypeReference<>() {});

        SystemResourceMetricsMessage body = message.getBody();
        KafkaCommonMessage.Header header = message.getHeader();
        String server = body.serverName();

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

//            [SEQ-CHECK] Server-01:
//              → [CPU] 67.123% (info) @ 2025-04-29T12:00:00
//              → [Memory] 73.892% (warning) @ 2025-04-29T12:00:00
//              → [Disk] 91.002% (critical) @ 2025-04-29T12:00:00
//
//            [SEQ-CHECK] Server-02:
//              → [CPU] 54.002% (info) @ 2025-04-29T12:00:01
//              → [Memory] 63.882% (info) @ 2025-04-29T12:00:01

            // 출력 후 초기화
            serverMessageMap.get(server).clear();
        }
    }


}
