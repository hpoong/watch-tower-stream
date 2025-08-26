package com.hopoong.user.event;

import com.hopoong.avro.record.user.UserActionEventRecord;
import com.hopoong.user.adapter.kafka.UserActionEventKafkaPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserActionEventHandlerImpl implements UserActionEventHandler {

    private final UserActionEventKafkaPublisher<UserActionEventRecord> kafkaPublisher;

    @Override
    public void handleSystemResourceMetricsEvent(UserActionEventRecord body) {
        kafkaPublisher.accept(body);
    }
}
