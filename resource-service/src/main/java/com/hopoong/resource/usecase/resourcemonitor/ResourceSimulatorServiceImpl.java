package com.hopoong.resource.usecase.resourcemonitor;

import com.hopoong.resource.api.resourcemonitor.model.ResourceContext;
import com.hopoong.resource.api.resourcemonitor.model.ResourceUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceSimulatorServiceImpl implements ResourceSimulatorService {

    private final ResourcePatternService patternService;
    private final ResourceContext context;

    private static final double SPIKE_PROBABILITY = 0.05; // 5% 확률로 스파이크 발생
    private static final double CPU_SPIKE_AMPLITUDE = 30.0;
    private static final double MEMORY_SPIKE_AMPLITUDE = 25.0;
    private static final double DISK_SPIKE_AMPLITUDE = 20.0;


    private double gaussian(Random r, double stdDev) {
        return r.nextGaussian() * stdDev;
    }

    public ResourceUsage simulate() {
        LocalDateTime now = LocalDateTime.now();
        Random random = new Random(System.currentTimeMillis() / (60 * 1000)); // 1분 단위

        // 베이스 패턴
        double baseCpu = patternService.getBaseCpu(now);
        double baseMem = patternService.getBaseMemory(now);
        double baseDisk = patternService.getBaseDisk(now);

        // 노이즈 + 패턴
        double cpu = baseCpu + gaussian(random, 5.0);
        double memory = baseMem + gaussian(random, 3.0) + cpu * 0.05;
        double disk = baseDisk + gaussian(random, 1.5);

        // 스파이크 주입: 확률적으로 이상치 발생
        if (random.nextDouble() < SPIKE_PROBABILITY) {
            cpu += CPU_SPIKE_AMPLITUDE;
        }
        if (random.nextDouble() < SPIKE_PROBABILITY) {
            memory += MEMORY_SPIKE_AMPLITUDE;
        }
        if (random.nextDouble() < SPIKE_PROBABILITY) {
            disk += DISK_SPIKE_AMPLITUDE;
        }

        // 클리핑
        cpu = Math.max(0, Math.min(100, cpu));
        memory = Math.max(0, Math.min(100, memory));
        disk = Math.max(0, Math.min(100, disk));

        // 상태 업데이트
        context.setLastCpu(cpu);
        context.setLastMemory(memory);
        context.setLastDisk(disk);

        return new ResourceUsage(cpu, memory, disk, now);
    }
}