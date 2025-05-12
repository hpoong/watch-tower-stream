package com.hopoong.audit.common.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.To;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DelayedForwarder implements Transformer<String, String, KeyValue<String, String>> {

    private final String targetTopic;
    private final Duration delay;
    private KafkaProducer<String, String> producer;
    private ProcessorContext context;
    private final Map<String, String> scheduledMessages = new HashMap<>();

    public DelayedForwarder(String targetTopic, Duration delay) {
        this.targetTopic = targetTopic;
        this.delay = delay;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        this.producer = new KafkaProducer<>(props);

        context.schedule(delay, PunctuationType.WALL_CLOCK_TIME, timestamp -> {
            for (Map.Entry<String, String> entry : scheduledMessages.entrySet()) {
                producer.send(new ProducerRecord<>(targetTopic, entry.getKey(), entry.getValue()));
            }
            scheduledMessages.clear();
        });
    }

    @Override
    public KeyValue<String, String> transform(String key, String value) {
        scheduledMessages.put(key, value);
        return null;
    }

    @Override
    public void close() {
        if (producer != null) {
            producer.close();
        }
    }
}
