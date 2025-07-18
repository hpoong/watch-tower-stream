package com.hopoong.audit.persistence.document;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.hopoong.audit.adapter.kafka.resourcemetric.model.AvgMax;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StatisticalMetricDocument {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime timestamp;
    private Double averageValue;
    private Double totalValue;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime minTimestamp;
    private Double minValue;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime maxTimestamp;
    private Double maxValue;
    private String resourceName;
    private String serverName;


    public static StatisticalMetricDocument toStatisticalMetricDocument(AvgMax value) {
        return StatisticalMetricDocument.builder()
                .averageValue(value.getAverageValue())
                .minTimestamp(value.getMinTimestamp())
                .minValue(value.getMinValue())
                .maxTimestamp(value.getMaxTimestamp())
                .maxValue(value.getMaxValue())
                .totalValue(value.getTotalValue())
                .timestamp(value.getTimestamp())
                .resourceName(value.getResourceName())
                .serverName(value.getServerName())
                .build();
    }
}