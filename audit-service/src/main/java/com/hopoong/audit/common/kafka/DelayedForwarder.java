package com.hopoong.audit.common.kafka;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;

import java.time.Duration;

public class DelayedForwarder implements Transformer<String, String, KeyValue<String, String>> {

    private final String targetTopic;
    private final Duration delay;
    private ProcessorContext context;

    public DelayedForwarder(String targetTopic, Duration delay) {
        this.targetTopic = targetTopic;
        this.delay = delay;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
    }

    @Override
    public KeyValue<String, String> transform(String key, String value) {
        long scheduledTime = System.currentTimeMillis() + delay.toMillis();

        // 예약 전달
        context.schedule(delay, PunctuationType.WALL_CLOCK_TIME, timestamp -> {
            context.forward(key, value);
        });

        return null; // 지금은 안 보내고 나중에 보내도록 예약
    }

    @Override
    public void close() {
    }
}
