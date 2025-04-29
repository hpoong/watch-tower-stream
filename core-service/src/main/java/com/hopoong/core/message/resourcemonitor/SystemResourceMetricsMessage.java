package com.hopoong.core.message.resourcemonitor;


public record SystemResourceMetricsMessage(
        String resourceName,
        double usagePercent,
        String alertLevel,
        String serverName,
        String ipAddress,
        String timestamp
) {}
