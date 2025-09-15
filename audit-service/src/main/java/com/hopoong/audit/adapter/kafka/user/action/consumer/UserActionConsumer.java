package com.hopoong.audit.adapter.kafka.user.action.consumer;

import com.hopoong.audit.usecase.user.action.UserActionService;
import com.hopoong.avro.message.UserActionEventMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final UserActionService userActionService;


    @KafkaListener(
            topics = KafkaTopicManager.USER_ACTION_EVENTS,
            groupId = "user-action-events-group",
            containerFactory = "avroKafkaListenerContainerFactory",
            concurrency = "1"
    )
    public void userActionEventConsume(ConsumerRecord<String, UserActionEventMessage> record) throws Exception {
        userActionService.registerUserAction(record);
    }

}