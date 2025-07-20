package com.hopoong.resource.batch;

import com.hopoong.core.util.RandomUtil;
import com.hopoong.resource.api.resourcemonitor.model.ResourceUsage;
import com.hopoong.resource.event.ResourceMonitorEventHandler;
import com.hopoong.resource.usecase.resourcemonitor.ResourcePatternService;
import com.hopoong.resource.usecase.resourcemonitor.ResourceSimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceMonitorBatchService {

    private final ResourceMonitorEventHandler resourceMonitorEventHandler;
    private final ResourceSimulatorService resourceSimulatorService;
    private final ResourcePatternService resourcePatternService;


    @Scheduled(fixedRate = 60000)
    public void monitorResources() {
        String serverName = RandomUtil.getRandomServerName();
        String ipAddress = RandomUtil.getRandomIpAddress();

        ResourceUsage simulate = resourceSimulatorService.simulate();
        double cpuUsage = simulate.getCpu();
        double memoryUsage = simulate.getMemory();
        double diskUsage = simulate.getDisk();

        String cpuLevel = resourcePatternService.determineAlert(cpuUsage);
        String memLevel = resourcePatternService.determineAlert(memoryUsage);
        String diskLevel = resourcePatternService.determineAlert(diskUsage);

        resourceMonitorEventHandler.handleSystemResourceMetricsEvent("CPU", cpuUsage, cpuLevel, serverName, ipAddress);
        resourceMonitorEventHandler.handleSystemResourceMetricsEvent("Memory", memoryUsage, memLevel, serverName, ipAddress);
        resourceMonitorEventHandler.handleSystemResourceMetricsEvent("Disk", diskUsage, diskLevel, serverName, ipAddress);
    }

}
