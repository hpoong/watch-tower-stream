package com.hopoong.audit.adapter.kafka.resourcemetric.transformer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsErrorMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.springframework.kafka.core.KafkaTemplate;


@Slf4j
public class ErrorForwarderTransformer implements Transformer<String, String, KeyValue<String, String>> {


    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String targetTopic;

    public ErrorForwarderTransformer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper, String targetTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.targetTopic = targetTopic;
    }

    @Override
    public void init(ProcessorContext context) { }

    @Override
    public KeyValue<String, String> transform(String key, String value) {
        try {
            KafkaCommonMessage<SystemResourceMetricsMessage> originalMessage =
                    objectMapper.readValue(value, new TypeReference<>() {});

            SystemResourceMetricsErrorMessage errorMessage =
                    SystemResourceMetricsErrorMessage.builder()
                            .orgMessage(originalMessage.getBody())
                            .errorType("DUPLICATE")
                            .build();

            KafkaCommonMessage<?> kafkaMessage = KafkaCommonMessage.builder()
                    .header(originalMessage.getHeader())
                    .body(errorMessage)
                    .build();

            String payload = objectMapper.writeValueAsString(kafkaMessage);
            kafkaTemplate.send(targetTopic, payload);

        } catch (Exception e) {
            log.error("[ErrorForwarderTransformer Transformer] JSON 파싱 실패. value: {}, error: {}", value, e.getMessage());
        }

        return null;
    }

    @Override
    public void close() { }
}
