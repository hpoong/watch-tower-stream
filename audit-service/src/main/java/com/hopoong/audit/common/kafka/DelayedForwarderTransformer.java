package com.hopoong.audit.common.kafka;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DelayedForwarderTransformer implements Transformer<String, String, KeyValue<String, String>> {

    private final String targetTopic;
    private final Duration delay;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private ProcessorContext context;
    private final Map<String, String> scheduledMessages = new HashMap<>();

    public DelayedForwarderTransformer(String targetTopic, Duration delay, KafkaTemplate<String, Object> kafkaTemplate) {
        this.targetTopic = targetTopic;
        this.delay = delay;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        context.schedule(delay, PunctuationType.WALL_CLOCK_TIME, timestamp -> {
            for (Map.Entry<String, String> entry : scheduledMessages.entrySet()) {
                kafkaTemplate.send(targetTopic, entry.getKey(), entry.getValue());
            }
            scheduledMessages.clear();
        });
    }

    @Override
    public KeyValue<String, String> transform(String key, String value) {
        try {
            scheduledMessages.put(key, value);
        } catch (Exception e) {
            throw new RuntimeException("DelayedForwarder 변환 실패: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void close() { }
}
