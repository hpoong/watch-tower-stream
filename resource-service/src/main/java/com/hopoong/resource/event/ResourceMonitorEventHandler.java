package com.hopoong.resource.event;

public interface ResourceMonitorEventHandler {
    void handleSystemResourceMetricsEvent(String resourceName, double usage, String level, String serverName, String ip);
}
