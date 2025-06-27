package com.hopoong.resource.api.resourcemonitor.model;

public record ResourceMonitorRequest(
        String topic,
        String serverName,
        String resourceName,
        long startTimeMillis,
        long endTimeMillis
) {}