package com.hopoong.resource.usecase.resourcemonitor;

import com.hopoong.resource.api.resourcemonitor.model.ResourceMonitorRequest;

public interface ResourceMonitorService {

    // CPU
    double measureCpuUsage();

    // Memory
    double measureMemoryUsage();

    // Disk
    double measureDiskUsage();

    String determineAlert(double usage);

    void replayServerMetrics(ResourceMonitorRequest request);
}
