package com.hopoong.audit.adapter.kafka.resourcemetric;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.common.kafka.BatchedDbWriterTransformer;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class ReProcesKafkaConsumer {

    private final ObjectMapper objectMapper;


    @Bean
    public KStream<String, String> reprocessStream(StreamsBuilder builder) {
        KStream<String, String> stream = builder.stream(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC + ".REPROCESS");
        stream.transform(() -> new BatchedDbWriterTransformer(2), Named.as("BatchToDb"));
        return stream;
    }


//    @Bean
//    public KStream<String, String> reprocessStream(StreamsBuilder builder) {
//
//        KStream<String, String> dlqStream = builder.stream(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC + ".DLQ");
//
//        KStream<String, String>[] branches = dlqStream.branch(
//                (key, value) -> !isTraceIdInResourceMonitor(value),
//                (key, value) -> true
//        );
//
//        // 정상 메시지만 딜레이 후 전송
//        branches[0]
//                .peek((key, value) -> log.info("TEST: {}", testEvent(value)));
//
//        // 중복된 메시지는 DB 저장 또는 로그 출력
//        branches[1]
//                .peek((key, value) -> log.info("TEST: {}", testEvent(value)));
//
//        return dlqStream;
//    }
//
//    private String testEvent(String value) {
//        System.out.println(value);
//        try {
//            KafkaCommonMessage<SystemResourceMetricsMessage> message =
//                    objectMapper.readValue(value, new TypeReference<>() {});
//
//            System.out.println(message.getBody().alertLevel());
//            System.out.println(message.getBody().ipAddress());
//
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//
//        return "";
//    }
//
//    private boolean isTraceIdInResourceMonitor(String value) {
//        return true;
//    }

}
