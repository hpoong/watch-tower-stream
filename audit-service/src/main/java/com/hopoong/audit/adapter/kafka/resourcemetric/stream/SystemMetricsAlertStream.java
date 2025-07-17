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
public class SystemMetricsAlertStream  extends AbstractMetricsStream {

    public SystemMetricsAlertStream(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /*
     * 리소스 사용량 임계치 초과시 알람처리
     */
    @Bean
    public KStream<String, String> systemMetricsThresholdAlertStream(StreamsBuilder builder) {

        GenericJsonSerde<KafkaCommonMessage<SystemResourceMetricsMessage>> serde
                = createSerde(new TypeReference<KafkaCommonMessage<SystemResourceMetricsMessage>>() {});

        KStream<String, KafkaCommonMessage<SystemResourceMetricsMessage>> stream
                = createResourceMetricsStream(builder, KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC, serde);

        KTable<String, KafkaCommonMessage<SystemThresholdMessage>> parsedThresholdKTable
                = parseKTable(builder, KafkaTopicManager.SYSTEM_THRESHOLD_TOPIC,
                    new TypeReference<KafkaCommonMessage<SystemThresholdMessage>>() {});

        // JOIN 수행
        KStream<String, Double> alertStream = stream.join(
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
