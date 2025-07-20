package com.hopoong.resource.usecase.resourcemonitor;

import java.time.LocalDateTime;

public interface ResourcePatternService {

    /**
     * 현재 시간에 따라 기본 CPU 사용량을 반환합니다.
     *
     * @param now 현재 시간
     * @return 기본 CPU 사용량 (0~100 범위 내 예상)
     */
    double getBaseCpu(LocalDateTime now);

    /**
     * 현재 시간에 따라 기본 메모리 사용량을 반환합니다.
     *
     * @param now 현재 시간
     * @return 기본 메모리 사용량 (0~100 범위 내 예상)
     */
    double getBaseMemory(LocalDateTime now);

    /**
     * 현재 시간에 따라 기본 디스크 사용량을 반환합니다.
     *
     * @param now 현재 시간
     * @return 기본 디스크 사용량 (0~100 범위 내 예상)
     */
    double getBaseDisk(LocalDateTime now);

    String determineAlert(double usage);
}