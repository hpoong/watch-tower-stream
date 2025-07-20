package com.hopoong.resource.usecase.resourcemonitor;

import com.hopoong.resource.api.resourcemonitor.model.ResourceMonitorRequest;

public interface ResourceMonitorService {

    void replayServerMetrics(ResourceMonitorRequest request);
}
