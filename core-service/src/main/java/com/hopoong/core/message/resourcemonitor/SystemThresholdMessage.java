package com.hopoong.core.message.resourcemonitor;


import lombok.Builder;

@Builder
public record SystemThresholdMessage(
        double thresholdValue,
        String serverName,
        String resourceName
) {}
