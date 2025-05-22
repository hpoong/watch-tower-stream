package com.hopoong.audit.common.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.usecase.resourcemonitor.ResourceMonitorService;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BatchedDbWriterTransformer implements Transformer<String, String, KeyValue<String, String>> {

    private final int batchSize;
    private final List<KafkaCommonMessage<SystemResourceMetricsMessage>> buffer = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private final ResourceMonitorService resourceMonitorService;

    public BatchedDbWriterTransformer(int batchSize, ObjectMapper objectMapper, ResourceMonitorService resourceMonitorService) {
        this.batchSize = batchSize;
        this.objectMapper = objectMapper;
        this.resourceMonitorService = resourceMonitorService;
    }

    @Override
    public void init(ProcessorContext context) { }

    @Override
    public KeyValue<String, String> transform(String key, String value) {
        try {

            KafkaCommonMessage<SystemResourceMetricsMessage> parsed =
                    objectMapper.readValue(value, new TypeReference<>() {});

            buffer.add(parsed);

            if (buffer.size() >= batchSize) {
                resourceMonitorService.insertSystemResourceMetricsBulk(buffer);
                buffer.clear();
            }
        } catch (Exception e) {
            log.error("[Transformer] Failed to process message: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void close() {  }
}
