package com.hopoong.resource.batch;

import com.hopoong.resource.app.system.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceMonitorBatchService {

    private final SystemService systemService;

    @Scheduled(fixedRate = 60000)
    public void monitorResources() {
        systemService.checkCpuUsage();
        systemService.checkDiskUsage();
        systemService.checkMemoryUsage();
    }
}
