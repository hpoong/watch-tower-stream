package com.hopoong.resource.batch;

import com.hopoong.resource.app.resourcemonitor.ResourceMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceMonitorBatchService {

    private final ResourceMonitorService resourceMonitorService;

    @Scheduled(fixedRate = 60000)
    public void monitorResources() {
        resourceMonitorService.checkCpuUsage();
        resourceMonitorService.checkDiskUsage();
        resourceMonitorService.checkMemoryUsage();
    }
}
