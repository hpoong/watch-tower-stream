package com.hopoong.audit.adapter.kafka.resourcemetric.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.adapter.kafka.resourcemetric.model.AvgMax;
import com.hopoong.audit.common.exception.KafkaProcessingException;
import com.hopoong.audit.common.serde.AvgMaxSerde;
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
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
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
    public KStream<String, String> systemMetricsThresholdAlertStream(StreamsBuilder builder) {

        // system-resource-metrics : KStream
        GenericJsonSerde<KafkaCommonMessage<SystemResourceMetricsMessage>> resourceSerde =
                new GenericJsonSerde<>(objectMapper, new TypeReference<KafkaCommonMessage<SystemResourceMetricsMessage>>() {});

        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> resourceMetricsStream = builder.stream(
                KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC,
                Consumed.with(Serdes.String(), resourceSerde)
        );

//        resourceMetricsStream.foreach((k, v) -> log.info(">>>>>>>>>>>> 1  key = {} value = {}", k, v));

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

        parsedThresholdKTable.toStream().foreach((k, v) -> log.info(">>>>>>>>>>>> 2  key = {} value = {}", k, v));

        // JOIN 수행
        KStream<String, Double> alertStream = resourceMetricsStream.join(
            parsedThresholdKTable,
            (resource, threshold) -> {
                if(resource.getBody().usagePercent() > threshold.getBody().thresholdValue()) {
                    return resource.getBody().usagePercent();
                }
                return null;
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


    @Bean
    public KStream<String, String> systemMetricsThresholdAvgMaxOneMinStream(StreamsBuilder builder) {

        // system-resource-metrics : KStream
        GenericJsonSerde<KafkaCommonMessage<SystemResourceMetricsMessage>> resourceSerde =
                new GenericJsonSerde<>(objectMapper, new TypeReference<KafkaCommonMessage<SystemResourceMetricsMessage>>() {});

        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> resourceMetricsStream = builder.stream(
                KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC,
                Consumed.with(Serdes.String(), resourceSerde)
        );

        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> cpuStream =
                resourceMetricsStream.filter((key, value) ->
                        value.getBody().resourceName().equalsIgnoreCase("CPU")
                );


        TimeWindows oneMinute = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1));
        TimeWindows fiveMinutes = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5));
        TimeWindows tenMinutes = TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(10));

        KTable<Windowed<String>, AvgMax> avgMaxOneMin = cpuStream
            .groupByKey()
            .windowedBy(fiveMinutes)
            .aggregate(
                    AvgMax::new,
                    (key, value, aggregate) -> aggregate.add(value.getBody().usagePercent()),
                    Materialized.with(Serdes.String(), new AvgMaxSerde())
            );

        avgMaxOneMin.toStream().foreach((windowedKey, value) -> {
            log.info("[5분] CPU 평균 = {}, 최대 = {}", value.avg(), value.max());
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
