package com.hopoong.resource.batch;

import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.util.RandomUtil;
import com.hopoong.resource.app.resourcemonitor.ResourceMonitorService;
import com.hopoong.resource.event.ResourceMonitorEventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceMonitorBatchService {

    private final ResourceMonitorService resourceMonitorService;
    private final ResourceMonitorEventHandler resourceMonitorEventHandler;


    @Scheduled(fixedRate = 60000)
    public void monitorResources() {
        String serverName = RandomUtil.getRandomServerName();
        String ipAddress = RandomUtil.getRandomIpAddress();

        double cpuUsage = resourceMonitorService.measureCpuUsage();
        double memoryUsage = resourceMonitorService.measureMemoryUsage();
        double diskUsage = resourceMonitorService.measureDiskUsage();

        String cpuLevel = resourceMonitorService.determineAlert(cpuUsage);
        String memLevel = resourceMonitorService.determineAlert(memoryUsage);
        String diskLevel = resourceMonitorService.determineAlert(diskUsage);

        resourceMonitorEventHandler.handleSystemResourceMetricsEvent("CPU", cpuUsage, cpuLevel, serverName, ipAddress);
        resourceMonitorEventHandler.handleSystemResourceMetricsEvent("Memory", memoryUsage, memLevel, serverName, ipAddress);
        resourceMonitorEventHandler.handleSystemResourceMetricsEvent("Disk", diskUsage, diskLevel, serverName, ipAddress);
    }

}
