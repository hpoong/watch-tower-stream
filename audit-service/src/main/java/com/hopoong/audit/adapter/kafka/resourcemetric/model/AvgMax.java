package com.hopoong.audit.adapter.kafka.resourcemetric.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AvgMax {

    private LocalDateTime timestamp;
    private Double averageValue = 0.0;
    private Double totalValue = 0.0;
    private LocalDateTime minTimestamp;
    private Double minValue = 0.0;
    private LocalDateTime maxTimestamp;
    private Double maxValue = 0.0;
    private String resourceName;
    private String serverName;
    private long count = 0;


    public AvgMax add(double usage, LocalDateTime usageTimestamp) {
        totalValue += usage;
        count++;
        averageValue = totalValue / count;

        if (count == 1) {
            minValue = maxValue = usage;
            minTimestamp = maxTimestamp = usageTimestamp;
        } else {
            if (usage < minValue) {
                minValue = usage;
                minTimestamp = usageTimestamp;
            }
            if (usage > maxValue) {
                maxValue = usage;
                maxTimestamp = usageTimestamp;
            }
        }

        return this;
    }

    public double avg() { return count == 0 ? 0 : totalValue / count; }
    public double max() { return maxValue; }
    public double min() { return minValue; }
    public LocalDateTime getMaxTimestamp() { return maxTimestamp; }
    public LocalDateTime getMinTimestamp() { return minTimestamp; }
}
