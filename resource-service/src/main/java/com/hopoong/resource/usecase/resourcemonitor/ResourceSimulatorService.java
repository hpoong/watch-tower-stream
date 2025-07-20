package com.hopoong.resource.usecase.resourcemonitor;

import com.hopoong.resource.api.resourcemonitor.model.ResourceUsage;

public interface ResourceSimulatorService {
    /**
     * 리소스 사용량을 시뮬레이션합니다.
     *
     * @return 현재 시간 기준 시뮬레이션된 ResourceUsage 객체
     */
    ResourceUsage simulate();
}