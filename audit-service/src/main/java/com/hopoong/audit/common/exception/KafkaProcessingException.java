package com.hopoong.audit.common.exception;


import lombok.Data;

@Data
public class KafkaProcessingException extends RuntimeException {

    private final String topic;
    private final int partition;
    private final long offset;
    private final String traceId;

    public KafkaProcessingException(String topic, int partition, long offset, String traceId) {
        super();
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.traceId = traceId;
    }

    public KafkaProcessingException(String message, Throwable cause, String topic, int partition, long offset, String traceId) {
        super(message, cause);
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.traceId = traceId;
    }
}
