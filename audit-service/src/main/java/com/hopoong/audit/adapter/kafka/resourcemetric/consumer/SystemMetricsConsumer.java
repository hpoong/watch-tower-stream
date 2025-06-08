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
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
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
                .selectKey((key, value) -> {
                    String serverName = value.getBody().serverName();
                    String resourceName = value.getBody().resourceName(); // resourceName 과 동일 의미
                    return serverName + ":" + resourceName;
                });

        keyedResourceStream.foreach((k, v) -> System.out.println(">>>>>>>>>>>> 3 " + k));



        // system-threshold : KTable
        GenericJsonSerde<KafkaCommonMessage<SystemThresholdMessage>> thresholdSerde =
                new GenericJsonSerde<>(objectMapper, new TypeReference<KafkaCommonMessage<SystemThresholdMessage>>() {});

        KTable<String, KafkaCommonMessage<SystemThresholdMessage>> thresholdKTable = builder.table(
                KafkaTopicManager.SYSTEM_THRESHOLD_TOPIC,
                Consumed.with(Serdes.String(), thresholdSerde)
        );

        thresholdKTable.toStream().foreach((k, v) -> System.out.println(">>>>>>>>>>>> 2  key = " + k + " value = " + v));

        // JOIN 수행
        KStream<String, String> alertStream = keyedResourceStream.join(
            thresholdKTable,
            (resource, threshold) -> {
//                if (threshold == null || threshold.getBody() == null) {
//                    return null;
//                }
                double usagePercent = resource.getBody().usagePercent();
                double thresholdValue = threshold.getBody().thresholdValue();

                if (usagePercent > thresholdValue) {
                    // 임계값 초과 → 알람 이벤트 생성 (여기서는 String 으로 예시)
                    return String.format("ALERT! server=%s resource=%s usage=%.2f%% threshold=%.2f%%",
                            resource.getBody().serverName(),
                            resource.getBody().resourceName(),
                            usagePercent,
                            thresholdValue
                    );
                } else {
                    // 임계 미만 → 알람 없음 (null 리턴 or "OK" 등으로 처리)
                    return null;
                }
            }
        ).filter((key, value) -> value != null); // null 제거 (알람만 남김)

//
//        // 알람 전송 → 다른 Kafka Topic 으로 보내기 (ex: SYSTEM_ALERT_TOPIC)
////        alertStream.to(KafkaTopicManager.SYSTEM_ALERT_TOPIC, Produced.with(Serdes.String(), Serdes.String()));
//
        alertStream
            .foreach((key, value) -> {
                log.info("==================================================");
                log.info("[alertStream] key = {}, value = {}", key, value);
                log.info("==================================================");
            });
//
//        return alertStream;


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
