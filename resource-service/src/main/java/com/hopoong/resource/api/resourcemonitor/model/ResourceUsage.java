package com.hopoong.resource.api.resourcemonitor.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ResourceUsage {
    private double cpu;
    private double memory;
    private double disk;
    private LocalDateTime timestamp;
}