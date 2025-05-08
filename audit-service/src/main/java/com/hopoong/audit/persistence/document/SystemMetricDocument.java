package com.hopoong.audit.persistence.document;

import com.fasterxml.jackson.annotation.JsonFormat;
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
}
