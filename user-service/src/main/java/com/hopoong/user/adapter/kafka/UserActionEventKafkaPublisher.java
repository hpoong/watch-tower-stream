package com.hopoong.user.adapter.kafka;

import com.hopoong.avro.common.CommonHeaderRecord;
import com.hopoong.avro.message.UserActionEventMessage;
import com.hopoong.avro.record.user.UserActionEventRecord;
import com.hopoong.core.topic.KafkaTopicManager;
import com.hopoong.core.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionEventKafkaPublisher<T> implements Consumer<UserActionEventRecord> {

    @Qualifier("avroKafkaTemplate")
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void accept(UserActionEventRecord message) {
        publish(message, KafkaTopicManager.USER_ACTION_EVENTS, "USER_ACTION_EVENTS");
    }

    public void publish(UserActionEventRecord message, String topic, String type) {
        try {
            UserActionEventMessage kafkaMessage = buildKafkaMessage(message, topic, type);
            String partitionKey = generatePartitionKey(message);
            kafkaTemplate.send(topic, partitionKey, kafkaMessage);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private UserActionEventMessage buildKafkaMessage(UserActionEventRecord message, String topic, String type) {

        CommonHeaderRecord header = CommonHeaderRecord.newBuilder()
                .setTopic(topic)
                .setType(type)
                .setTraceId(UUID.randomUUID().toString())
                .setTimestamp(TimeUtil.nowInstantUtc())
                .build();

        return UserActionEventMessage.newBuilder()
                .setBody(message)
                .setHeader(header)
                .build();
    }

    private String generatePartitionKey(UserActionEventRecord message) {
        return message.getId().toString();
    }
}