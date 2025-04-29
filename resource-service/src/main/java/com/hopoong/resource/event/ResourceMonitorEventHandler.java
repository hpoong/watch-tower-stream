package com.hopoong.resource.event;

import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;

public interface ResourceMonitorEventHandler {
    void handleSystemResourceMetricsEvent(SystemResourceMetricsMessage message);
}
