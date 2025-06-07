package com.hopoong.resource.api.threshold;

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

    @PostMapping("/register")
    public SuccessResponse registerThreshold(@RequestBody ThresholdRequest request) {
        systemThresholdService.registerThreshold(request);
        return new SuccessResponse(CommonResponseCodeEnum.SERVER, null);
    }

}

