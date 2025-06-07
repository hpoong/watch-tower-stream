package com.hopoong.resource.usecase.threshold;

import com.hopoong.resource.api.threshold.model.ThresholdRequest;

public interface SystemThresholdService {

    void registerThreshold(ThresholdRequest request);
}
