package com.hopoong.audit.config.kafka;


import com.hopoong.avro.message.UserActionEventMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

import java.time.Instant;

public class CreatedAtTimestampExtractor implements TimestampExtractor {

    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        Object v = record.value();
        if (v instanceof UserActionEventMessage u) {
            var body = u.getBody();
            Instant ts = body.getCreatedAt();
            if (ts != null) return ts.toEpochMilli();
        }

        return partitionTime;
    }
}
