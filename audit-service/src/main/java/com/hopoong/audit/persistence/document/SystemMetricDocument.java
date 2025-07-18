package com.hopoong.audit.persistence.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SystemMetricDocument {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime timestamp;
    private String resourceName;
    private Double usagePercent;
    private String alertLevel;
    private String serverName;
    private String ipAddress;
    private String traceId;



    public static SystemMetricDocument toSystemMetricDocument(KafkaCommonMessage<SystemResourceMetricsMessage> message) {
        return SystemMetricDocument.builder()
                .resourceName(message.getBody().resourceName())
                .usagePercent(message.getBody().usagePercent())
                .alertLevel(message.getBody().alertLevel())
                .serverName(message.getBody().serverName())
                .ipAddress(message.getBody().ipAddress())
                .timestamp(message.getHeader().getTimestamp())
                .traceId(message.getHeader().getTraceId())
                .build();
    }
}
