package com.hopoong.resource.app.resourcemonitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceMonitorServiceImpl implements ResourceMonitorService {
    private static final double STAGE1_THRESHOLD = 70.0;
    private static final double STAGE2_THRESHOLD = 80.0;
    private static final double STAGE3_THRESHOLD = 90.0;

    // CPU
    @Override
    public double measureCpuUsage() {
        double raw = ThreadLocalRandom.current().nextDouble(0.0, 1.0);
        return roundToThreeDecimalPlaces(raw * 100);
    }

    // Memory
    @Override
    public double measureMemoryUsage() {
        long total = 16L * 1024 * 1024 * 1024;
        long free = ThreadLocalRandom.current().nextLong(0, total);
        double usage = (total - free) / (double) total;
        return roundToThreeDecimalPlaces(usage * 100);
    }

    // Disk
    @Override
    public double measureDiskUsage() {
        long total = 500L * 1024 * 1024 * 1024;
        long usable = ThreadLocalRandom.current().nextLong(0, total);
        double usage = (total - usable) / (double) total;
        return roundToThreeDecimalPlaces(usage * 100);
    }

    public double roundToThreeDecimalPlaces(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    @Override
    public String determineAlert(double usage) {
        return usage >= STAGE3_THRESHOLD ? "critical" :
            usage >= STAGE2_THRESHOLD ? "warning" :
            usage >= STAGE1_THRESHOLD ? "info" : "normal";
    }


}
