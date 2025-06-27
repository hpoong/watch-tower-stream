package com.hopoong.resource.usecase.threshold;

import com.hopoong.core.message.resourcemonitor.SystemThresholdMessage;
import com.hopoong.resource.adapter.kafka.SystemThresholdKafkaPublisher;
import com.hopoong.resource.api.threshold.model.ThresholdRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemThresholdServiceImpl implements SystemThresholdService {

    private final SystemThresholdKafkaPublisher kafkaPublisher;

    @Override
    public void saveThreshold(ThresholdRequest request) {
        SystemThresholdMessage message = SystemThresholdMessage.builder()
                .thresholdValue(request.getThreshold())
                .serverName(request.getServerName())
                .resourceName(request.getResourceName())
                .build();

        kafkaPublisher.accept(message);
    }



}
