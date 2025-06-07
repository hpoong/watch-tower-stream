package com.hopoong.resource.api.threshold.model;

import lombok.Data;

@Data
public class ThresholdRequest {
    private String serverName;
    private String resourceName;
    private int threshold;
}