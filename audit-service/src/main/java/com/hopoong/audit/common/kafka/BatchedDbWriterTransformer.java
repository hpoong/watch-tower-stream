package com.hopoong.audit.common.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;

@Slf4j
public class BatchedDbWriterTransformer implements Transformer<String, String, KeyValue<String, String>> {

    private final int batchSize;

    public BatchedDbWriterTransformer(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public void init(ProcessorContext context) {
        log.info("[Transformer] init called");
    }

    @Override
    public KeyValue<String, String> transform(String key, String value) {
        log.info("[Transformer] transform called - key: {}, value: {}", key, value);
        return null;
    }

    @Override
    public void close() {
        log.info("[Transformer] close called");
    }
}