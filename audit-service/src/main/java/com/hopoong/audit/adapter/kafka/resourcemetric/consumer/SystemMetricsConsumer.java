package com.hopoong.audit.adapter.kafka.resourcemetric.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.common.exception.KafkaProcessingException;
import com.hopoong.audit.common.serde.GenericJsonSerde;
import com.hopoong.audit.usecase.resourcemonitor.ResourceMonitorService;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsErrorMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.message.resourcemonitor.SystemThresholdMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMetricsConsumer {

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

            // 출력용
            serverMessageMap
                    .computeIfAbsent(server, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(body);

            if (serverMessageMap.get(server).size() == 3) {
                List<SystemResourceMetricsMessage> sortedList = serverMessageMap.get(server).stream()
                        .sorted(Comparator.comparing(SystemResourceMetricsMessage::resourceName))
                        .toList();

                log.debug("[SEQ-CHECK] {}:", server);
                for (SystemResourceMetricsMessage m : sortedList) {
                    log.debug("  → [{}] {}% ({}) @ {}",
                            m.resourceName(),
                            String.format("%.3f", m.usagePercent()),
                            m.alertLevel(),
                            header.getTimestamp()
                    );
                }
                serverMessageMap.get(server).clear();

                // 강제 에러 처리
                resourceMonitorService.insertSystemResourceMetrics(message);
                throw new KafkaProcessingException(record.topic(), record.partition(), record.offset(), header.getTraceId());
            }

            resourceMonitorService.insertSystemResourceMetrics(message);

            ack.acknowledge();

        } catch (Exception e) {
            KafkaCommonMessage.Header header = extractHeaderSafely(record);
            log.error("[CONSUMER ERROR] Failed to process orgMessage", e);
            log.debug("  ↳ partition={}, offset={}, traceId={}, topic={}",
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

    @Bean
    public KStream<String, String> systemMetricsThresholdStream(StreamsBuilder builder) {

        // system-resource-metrics : KStream
        GenericJsonSerde<KafkaCommonMessage<SystemResourceMetricsMessage>> resourceSerde =
                new GenericJsonSerde<>(objectMapper, new TypeReference<KafkaCommonMessage<SystemResourceMetricsMessage>>() {});

        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> resourceMetricsStream = builder.stream(
                KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC,
                Consumed.with(Serdes.String(), resourceSerde)
        );

        resourceMetricsStream.foreach((k, v) -> System.out.println(">>>>>>>>>>>> 1 " + v.getBody().resourceName()));

        // resourceMetricsStream → KEY 재매핑 (serverName:resourceName) 으로 맞추기
        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> keyedResourceStream = resourceMetricsStream
                .filter((key, value) -> value != null && value.getBody() != null)
                .selectKey((key, value) -> {
                    String serverName = value.getBody().serverName();
                    String resourceName = value.getBody().resourceName();
                    return serverName + ":" + resourceName;
                });

        keyedResourceStream.foreach((k, v) -> System.out.println(">>>>>>>>>>>> 3  key = " + k + " value = " + v));

        // system-threshold : KTable
        KTable<String, String> thresholdKTable = builder.table(
                KafkaTopicManager.SYSTEM_THRESHOLD_TOPIC,
                Consumed.with(Serdes.String(), Serdes.String())
        );

        KTable<String, KafkaCommonMessage<SystemThresholdMessage>> parsedThresholdKTable = thresholdKTable.mapValues(value -> {
            try {
                String jsonString = objectMapper.readValue(value, String.class);
                return objectMapper.readValue(jsonString, new TypeReference<KafkaCommonMessage<SystemThresholdMessage>>() {});
            } catch (Exception e) {
                log.error("Failed to deserialize threshold message: {}", value, e);
                return null;
            }
        });

        parsedThresholdKTable.toStream().foreach((k, v) -> System.out.println(">>>>>>>>>>>> 2  key = " + k + " value = " + v));

        // JOIN 수행
        KStream<String, String> alertStream = keyedResourceStream.join(
            parsedThresholdKTable,
            (resource, threshold) -> {
                System.out.println(">>>>>>>>>>>>>>>>>>> ??? ");
                return "";
            }
        ).filter((key, value) -> value != null);


        alertStream
            .foreach((key, value) -> {
                log.info("==================================================");
                log.info("[alertStream] key = {}, value = {}", key, value);
                log.info("==================================================");
            });

        return null;
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
