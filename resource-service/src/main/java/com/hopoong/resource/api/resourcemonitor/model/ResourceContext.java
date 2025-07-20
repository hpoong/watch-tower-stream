package com.hopoong.resource.api.resourcemonitor.model;


import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class ResourceContext {
    private double lastCpu = 40.0;
    private double lastMemory = 50.0;
    private double lastDisk = 60.0;
}