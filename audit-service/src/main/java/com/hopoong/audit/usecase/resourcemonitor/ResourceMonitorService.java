package com.hopoong.audit.usecase.resourcemonitor;

import com.hopoong.audit.adapter.kafka.resourcemetric.model.AvgMax;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;

import java.io.IOException;
import java.util.List;

public interface ResourceMonitorService {

    void saveSystemResourceMetrics(KafkaCommonMessage<SystemResourceMetricsMessage> message) throws IOException;

    boolean existsByTraceId(String traceId) throws IOException;

    void saveSystemResourceMetricsBulk(List<KafkaCommonMessage<SystemResourceMetricsMessage>> messages) throws IOException;

    void saveSystemResourceMetrics5Min(AvgMax value) throws IOException;
}
