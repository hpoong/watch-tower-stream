package com.hopoong.audit.adapter.kafka.resourcemetric.transformer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.usecase.resourcemonitor.ResourceMonitorService;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.util.TimeUtil;
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

    private final int batchSize;
    private final List<KafkaCommonMessage<SystemResourceMetricsMessage>> buffer = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private final ResourceMonitorService resourceMonitorService;
    private ScheduledExecutorService scheduler;
    private ProcessorContext context;


    public BatchedDbWriterTransformer(int batchSize, ObjectMapper objectMapper, ResourceMonitorService resourceMonitorService) {
        this.batchSize = batchSize;
        this.objectMapper = objectMapper;
        this.resourceMonitorService = resourceMonitorService;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;

        scheduler = Executors.newScheduledThreadPool(1);

        // 23:59
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = now.withHour(23).withMinute(59).withSecond(0);
        if(now.isAfter(target)) {
            target = target.plusDays(1);
        }

        long initialDelay = Duration.between(now, target).toMillis();
        scheduler.scheduleAtFixedRate(() -> {
            log.info("flushBuffer 동작 시간 ::: {}", TimeUtil.getFormattedTimestamp());
            this.flushBuffer();
        }, initialDelay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }



    @Override
    public KeyValue<String, String> transform(String key, String value) {
        try {

            KafkaCommonMessage<SystemResourceMetricsMessage> parsed =
                    objectMapper.readValue(value, new TypeReference<>() {});

            synchronized (buffer) {
                buffer.add(parsed);
                if (buffer.size() >= batchSize) {
                    flushBuffer();
                }
            }
        } catch (Exception e) {
            log.error("[BatchedDbWriterTransformer Transformer] JSON 파싱 실패. value: {}, error: {}", value, e.getMessage());
        }
        return null;
    }

    @Override
    public void close() {
        flushBuffer();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }



    private void flushBuffer() {
        synchronized (buffer) {
            if (!buffer.isEmpty()) {
                try {
                    resourceMonitorService.insertSystemResourceMetricsBulk(new ArrayList<>(buffer));
                    buffer.clear();
                } catch (IOException e) {
                    log.error("[BatchedDbWriterTransformer FlushBuffer] 네트워크 또는 시스템 오류 {}", e.getMessage());
                }
            }
        }
    }



}
