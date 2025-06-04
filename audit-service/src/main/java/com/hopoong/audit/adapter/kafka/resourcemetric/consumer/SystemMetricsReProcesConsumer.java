package com.hopoong.audit.adapter.kafka.resourcemetric.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.adapter.kafka.resourcemetric.transformer.BatchedDbWriterTransformer;
import com.hopoong.audit.usecase.resourcemonitor.ResourceMonitorService;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMetricsReProcesConsumer {

    private final ObjectMapper objectMapper;
    private final ResourceMonitorService resourceMonitorService;

    @Bean
    public KStream<String, String> reprocessStream(StreamsBuilder builder) {
        KStream<String, String> stream = builder.stream(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_REPROCESS_TOPIC);
        stream
            .peek((key, value) -> log.info(":::::::::::::::::::::: {} Consume", KafkaTopicManager.SYSTEM_RESOURCE_METRICS_REPROCESS_TOPIC))
            .transform(() -> new BatchedDbWriterTransformer(10000, objectMapper, resourceMonitorService));

        return stream;
    }


}
