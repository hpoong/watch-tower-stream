package com.hopoong.resource.app.system;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

@Slf4j
@Service
public class SystemServiceImpl implements SystemService {
    private static final double CPU_USAGE_THRESHOLD = 0.9;
    private static final double MEMORY_USAGE_THRESHOLD = 0.9;
    private static final double DISK_USAGE_THRESHOLD = 0.9;


    // CPU
    @Override
    public void checkCpuUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            double cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad();
            if (cpuLoad >= CPU_USAGE_THRESHOLD) {
                log.warn("[ALERT] CPU usage high: {}%", cpuLoad * 100);
            }
        }
    }

    // Memory
    @Override
    public void checkMemoryUsage() {
        com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean)
                ManagementFactory.getOperatingSystemMXBean();
        double totalMemory = osBean.getTotalPhysicalMemorySize();
        double freeMemory = osBean.getFreePhysicalMemorySize();
        double usage = (totalMemory - freeMemory) / totalMemory;
        if (usage >= MEMORY_USAGE_THRESHOLD) {
            log.warn("[ALERT] Memory usage high: {}%", usage * 100);
        }
    }

    // Disk
    @Override
    public void checkDiskUsage() {
        File root = new File("/");
        long totalSpace = root.getTotalSpace();
        long usableSpace = root.getUsableSpace();
        double usage = (totalSpace - usableSpace) / (double) totalSpace;
        if (usage >= DISK_USAGE_THRESHOLD) {
            log.warn("[ALERT] Disk usage high: {}%", usage * 100);
        }
    }

}
