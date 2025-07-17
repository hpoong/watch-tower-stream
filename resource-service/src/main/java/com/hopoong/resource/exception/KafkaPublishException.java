package com.hopoong.resource.exception;

public class KafkaPublishException extends RuntimeException {

    private final String topic;
    private final String type;

    public KafkaPublishException(String message, String topic, String type) {
        super(message);
        this.topic = topic;
        this.type = type;
    }

    public KafkaPublishException(String message, String topic, String type, Throwable cause) {
        super(message, cause);
        this.topic = topic;
        this.type = type;
    }

    public String getTopic() {
        return topic;
    }

    public String getType() {
        return type;
    }
}