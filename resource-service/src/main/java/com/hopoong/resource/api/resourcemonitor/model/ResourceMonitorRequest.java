package com.hopoong.resource.api.resourcemonitor.model;

public record ResourceMonitorRequest(
        String topic,
        String serverName,
        String resourceName,
        String startTime, // "2025-06-30 10:00:00" 형태
        String endTime    // "2025-06-30 12:00:00" 형태
) {}