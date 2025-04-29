package com.hopoong.resource.app.resourcemonitor;

import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.util.RandomUtil;
import com.hopoong.resource.event.ResourceMonitorEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceMonitorServiceImpl implements ResourceMonitorService {
    private static final double STAGE1_THRESHOLD = 0.6;
    private static final double STAGE2_THRESHOLD = 0.7;
    private static final double STAGE3_THRESHOLD = 0.8;

    private final ResourceMonitorEventHandler resourceMonitorEventHandler;

    // CPU
    @Override
    public void checkCpuUsage() {
        double cpuLoad = ThreadLocalRandom.current().nextDouble(0.0, 1.0);
        logUsage("CPU", cpuLoad);
    }

    // Memory
    @Override
    public void checkMemoryUsage() {
        double totalMemory = 16L * 1024 * 1024 * 1024; // 16GB
        double freeMemory = ThreadLocalRandom.current().nextLong(0, (long) totalMemory);
        double usage = (totalMemory - freeMemory) / totalMemory;
        logUsage("Memory", usage);
    }

    // Disk
    @Override
    public void checkDiskUsage() {
        long totalSpace = 500L * 1024 * 1024 * 1024; // 500GB
        long usableSpace = ThreadLocalRandom.current().nextLong(0, totalSpace);
        double usage = (totalSpace - usableSpace) / (double) totalSpace;
        logUsage("Disk", usage);
    }


    private void logUsage(String resourceName, double usage) {
        Double usagePercent = usage * 100;

        String alertLevel = usage >= STAGE3_THRESHOLD ? "critical" :
                usage >= STAGE2_THRESHOLD ? "warning" :
                usage >= STAGE1_THRESHOLD ? "info" : null;

        if(alertLevel != null) {
            resourceMonitorEventHandler.handleSystemResourceMetricsEvent(
                new SystemResourceMetricsMessage(
                        resourceName,
                        usagePercent,
                        alertLevel,
                        RandomUtil.getRandomServerName(),
                        RandomUtil.getRandomIpAddress()
                )
            );
        }
    }
}
