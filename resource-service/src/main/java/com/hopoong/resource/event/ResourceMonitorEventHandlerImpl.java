package com.hopoong.resource.event;

import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.resource.adapter.kafka.SystemMetricsKafkaPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceMonitorEventHandlerImpl implements ResourceMonitorEventHandler {

    private final SystemMetricsKafkaPublisher kafkaPublisher;

    // 시스템 리소스
    @Override
    public void handleSystemResourceMetricsEvent(SystemResourceMetricsMessage message) {
        kafkaPublisher.accept(message);
    }
}
