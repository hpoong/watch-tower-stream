package com.hopoong.core.message.resourcemonitor;


import lombok.Builder;

@Builder
public record SystemResourceMetricsErrorMessage(
        SystemResourceMetricsMessage orgMessage,
        String errorType
) implements ErrorBodyWrapper<SystemResourceMetricsMessage> {

    @Override
    public SystemResourceMetricsMessage originalMessage() {
        return orgMessage;
    }
}