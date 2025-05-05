package com.hopoong.audit.usecase.resourcemonitor;

import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;

import java.io.IOException;

public interface ResourceMonitorService {

    void insertSystemResourceMetrics(KafkaCommonMessage<SystemResourceMetricsMessage> message) throws IOException;
}
