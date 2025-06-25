package com.hopoong.audit.adapter.kafka.resourcemetric.stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.common.serde.GenericJsonSerde;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.message.resourcemonitor.SystemThresholdMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import com.hopoong.core.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMetricsAlertStream {

    private final ObjectMapper objectMapper;

    @Bean
    public KStream<String, String> systemMetricsThresholdAlertStream(StreamsBuilder builder) {

        // system-resource-metrics : KStream
        GenericJsonSerde<KafkaCommonMessage<SystemResourceMetricsMessage>> resourceSerde =
                new GenericJsonSerde<>(objectMapper, new TypeReference<KafkaCommonMessage<SystemResourceMetricsMessage>>() {});

        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> resourceMetricsStream = builder.stream(
                KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC,
                Consumed.with(Serdes.String(), resourceSerde)
        );

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
                    LoggerUtil.section(log, "[alertStream] key = %s, value = %s".formatted(key, value));
                });

        return null;
    }

}
