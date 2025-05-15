package com.hopoong.audit.adapter.kafka.resourcemetric;

import com.hopoong.audit.common.kafka.BatchedDbWriterTransformer;
import com.hopoong.core.topic.KafkaTopicManager;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.springframework.context.annotation.Bean;

public class ReProcesKafkaConsumer {

    @Bean
    public KStream<String, String> reprocessStream(StreamsBuilder builder) {
        KStream<String, String> stream = builder.stream(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC + ".REPROCESS");
        stream.transform(() -> new BatchedDbWriterTransformer(100), Named.as("BatchToDb"));
        return stream;
    }

}
