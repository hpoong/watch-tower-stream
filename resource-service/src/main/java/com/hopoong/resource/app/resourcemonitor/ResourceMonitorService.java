package com.hopoong.resource.app.resourcemonitor;

public interface ResourceMonitorService {

    // CPU
    void checkCpuUsage();

    // Memory
    void checkMemoryUsage();

    // Disk
    void checkDiskUsage();
}
