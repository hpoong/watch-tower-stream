package com.hopoong.audit.adapter.kafka.resourcemetric;

import com.hopoong.core.topic.KafkaTopicManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class ReProcesKafkaConsumer {

    @Bean
    public KStream<String, String> reprocessStream(StreamsBuilder builder) {

        KStream<String, String> stream = builder.stream(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC + ".REPROCESS");

        KStream<String, String>[] branches = stream.branch(
                (key, value) -> !isTraceIdInResourceMonitor(value),
                (key, value) -> true
        );

        branches[0]
                .peek((key, value) -> log.info("data: {}::: {}:::::", key, value));

        // 중복된 메시지는 DB 저장 또는 로그 출력
        branches[1]
                .peek((key, value) -> log.info("data: {}::: {}:::::", key, value));

        return stream;
    }

    private boolean isTraceIdInResourceMonitor(String value) {
        return true;
    }

}
