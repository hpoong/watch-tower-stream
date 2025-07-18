package com.hopoong.audit.adapter.kafka.resourcemetric.transformer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.usecase.resourcemonitor.ResourceMonitorService;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BatchedDbWriterTransformer implements Transformer<String, String, KeyValue<String, String>> {

    private final int batchSize = 10000;
    private final List<KafkaCommonMessage<SystemResourceMetricsMessage>> buffer = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private final ResourceMonitorService resourceMonitorService;
    private ScheduledExecutorService scheduler;
    private ProcessorContext context;

    public BatchedDbWriterTransformer(ObjectMapper objectMapper, ResourceMonitorService resourceMonitorService) {
        this.objectMapper = objectMapper;
        this.resourceMonitorService = resourceMonitorService;
    }

    // 스케줄러 설정
    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        startScheduler();
    }

    // 메시지 파싱 및 버퍼 추가
    @Override
    public KeyValue<String, String> transform(String key, String value) {
        try {
            KafkaCommonMessage<SystemResourceMetricsMessage> parsed = objectMapper.readValue(value, new TypeReference<>() {});
            addToBuffer(parsed);
        } catch (Exception e) {
            log.error("[BatchedDbWriterTransformer] JSON 파싱 실패. value: {}, error: {}", value, e.getMessage());
        }
        return null;
    }

    // 스케줄러 종료 및 버퍼 플러시
    @Override
    public void close() {
        safeFlushBuffer();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    // 버퍼 비우기 및 저장
    private void flushBuffer() throws IOException {
        List<KafkaCommonMessage<SystemResourceMetricsMessage>> toSave;
        synchronized (buffer) {
            if (buffer.isEmpty()) return;
            toSave = new ArrayList<>(buffer);
            buffer.clear();
        }
        resourceMonitorService.saveSystemResourceMetricsBulk(toSave);
    }

    private void startScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = now.withHour(23).withMinute(59).withSecond(0).withNano(0);
        if (now.isAfter(target)) {
            target = target.plusDays(1);
        }
        long initialDelay = Duration.between(now, target).toMillis();
        long period = TimeUnit.DAYS.toMillis(1);
        scheduler.scheduleAtFixedRate(this::safeFlushBuffer, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    private void addToBuffer(KafkaCommonMessage<SystemResourceMetricsMessage> message) {
        synchronized (buffer) {
            buffer.add(message);
            if (buffer.size() >= batchSize) {
                safeFlushBuffer();
            }
        }
    }

    private void safeFlushBuffer() {
        try {
            flushBuffer();
        } catch (Exception e) {
            log.error("[BatchedDbWriterTransformer] flushBuffer 실패: {}", e.getMessage());
        }
    }
}
