package com.hopoong.resource.app.system;

public interface SystemService {

    // CPU
    void checkCpuUsage();

    // Memory
    void checkMemoryUsage();

    // Disk
    void checkDiskUsage();
}
