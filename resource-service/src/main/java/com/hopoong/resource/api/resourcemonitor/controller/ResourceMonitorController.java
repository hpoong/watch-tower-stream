package com.hopoong.resource.api.resourcemonitor.controller;


import com.hopoong.core.response.CommonResponseCodeEnum;
import com.hopoong.core.response.SuccessResponse;
import com.hopoong.resource.api.resourcemonitor.model.ResourceMonitorRequest;
import com.hopoong.resource.usecase.resourcemonitor.ResourceMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/resourcemonitor")
@RequiredArgsConstructor
public class ResourceMonitorController {

    private final ResourceMonitorService resourceMonitorService;

    @PostMapping("/replay/system-metrics-by-time")
    public SuccessResponse replaySystemMetricsByTime(@RequestBody ResourceMonitorRequest request) {
        resourceMonitorService.replayServerMetrics(request);
        return new SuccessResponse(CommonResponseCodeEnum.SERVER, null);
    }

}
