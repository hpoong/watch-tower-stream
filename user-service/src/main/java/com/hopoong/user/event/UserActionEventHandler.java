package com.hopoong.user.event;

import com.hopoong.avro.record.user.UserActionEventRecord;

public interface UserActionEventHandler {
    void handleSystemResourceMetricsEvent(UserActionEventRecord body);
}
