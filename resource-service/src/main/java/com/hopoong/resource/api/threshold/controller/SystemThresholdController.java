package com.hopoong.resource.api.threshold.controller;

import com.hopoong.core.response.CommonResponseCodeEnum;
import com.hopoong.core.response.SuccessResponse;
import com.hopoong.resource.api.threshold.model.ThresholdRequest;
import com.hopoong.resource.usecase.threshold.SystemThresholdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/threshold")
@RequiredArgsConstructor
public class SystemThresholdController {

    private final SystemThresholdService systemThresholdService;

    @PostMapping("/save")
    public SuccessResponse saveThreshold(@RequestBody ThresholdRequest request) {
        systemThresholdService.saveThreshold(request);
        return new SuccessResponse(CommonResponseCodeEnum.SERVER, null);
    }
}

