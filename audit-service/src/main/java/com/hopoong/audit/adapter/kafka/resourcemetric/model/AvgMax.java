package com.hopoong.audit.adapter.kafka.resourcemetric.model;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
public class AvgMax {
    private double sum;
    private double max;
    private long count;

    public AvgMax add(double usage) {
        sum += usage;
        max = Math.max(max, usage);
        count++;
        return this;
    }

    public double avg() { return count == 0 ? 0 : sum / count; }
    public double max() { return max; }
}