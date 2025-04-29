package com.hopoong.resource.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


@Component
@RequiredArgsConstructor
public class SystemMetricsKafkaPublisher implements Consumer<SystemResourceMetricsMessage> {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public void accept(SystemResourceMetricsMessage message) {
        System.out.println(objectMapper.writeValueAsString(message));
    }
}
