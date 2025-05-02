package com.hopoong.core.message.resourcemonitor;


import lombok.Builder;

@Builder
public record SystemResourceMetricsMessage(
        String resourceName,
        double usagePercent,
        String alertLevel,
        String serverName,
        String ipAddress
) {}
