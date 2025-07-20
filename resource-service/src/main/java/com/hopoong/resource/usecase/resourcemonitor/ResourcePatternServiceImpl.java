package com.hopoong.resource.usecase.resourcemonitor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ResourcePatternServiceImpl implements ResourcePatternService {


    private static final double STAGE1_THRESHOLD = 70.0;
    private static final double STAGE2_THRESHOLD = 80.0;
    private static final double STAGE3_THRESHOLD = 90.0;

    @Override
    public double getBaseCpu(LocalDateTime now) {
        int hour = now.getHour();
        if (hour >= 9 && hour <= 18) return 50.0 + (hour - 9) * 1.5;
        if (hour >= 19 && hour <= 22) return 30.0 - (hour - 19) * 2.5;
        return 15.0 + (hour % 6 * 1.0);
    }

    @Override
    public double getBaseMemory(LocalDateTime now) {
        int hour = now.getHour();
        return (hour >= 9 && hour <= 18) ? 60.0 + (hour - 9) * 0.5 : 55.0;
    }

    @Override
    public double getBaseDisk(LocalDateTime now) {
        int dayOfYear = now.getDayOfYear();
        return 50.0 + (dayOfYear % 30) * 0.5 + (now.getHour() >= 9 && now.getHour() <= 18 ? 1.0 : 0.0);
    }

    @Override
    public String determineAlert(double usage) {
        return usage >= STAGE3_THRESHOLD ? "critical" :
                usage >= STAGE2_THRESHOLD ? "warning" :
                        usage >= STAGE1_THRESHOLD ? "info" : "normal";
    }
}
