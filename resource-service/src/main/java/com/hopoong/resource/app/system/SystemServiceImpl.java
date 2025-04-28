package com.hopoong.resource.app.system;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class SystemServiceImpl implements SystemService {
    private static final double STAGE1_THRESHOLD = 0.6;
    private static final double STAGE2_THRESHOLD = 0.7;
    private static final double STAGE3_THRESHOLD = 0.8;

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
        String result = String.format("%.3f", usage * 100);
        if (usage >= STAGE3_THRESHOLD) {
            log.warn("[ALERT-3단계] {} usage very high: {}%", resourceName, result);
        } else if (usage >= STAGE2_THRESHOLD) {
            log.warn("[ALERT-2단계] {} usage high: {}%", resourceName, result);
        } else if (usage >= STAGE1_THRESHOLD) {
            log.warn("[ALERT-1단계] {} usage warning: {}%", resourceName, result);
        }
    }

    private static String getRandomIpAddress() {
        int randomThirdOctet = ThreadLocalRandom.current().nextInt(0, 256);
        return String.format("192.168.%d.204", randomThirdOctet);
    }

    private static String getRandomServerName() {
        int number = ThreadLocalRandom.current().nextInt(0, 100);
        return String.format("Server-%02d", number);
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

}
