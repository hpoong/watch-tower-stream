package com.hopoong.resource.app.resourcemonitor;

public interface ResourceMonitorService {

    // CPU
    double measureCpuUsage();

    // Memory
    double measureMemoryUsage();

    // Disk
    double measureDiskUsage();

    String determineAlert(double usage);
}
