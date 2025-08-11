package com.hopoong.audit.adapter.kafka.resourcemetric.transformer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.usecase.resourcemonitor.ResourceMonitorService;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class BatchedDbWriterTransformer implements Transformer<String, String, KeyValue<String, String>> {

    private final int batchSize = 10000;
    private final List<KafkaCommonMessage<SystemResourceMetricsMessage>> buffer = new ArrayList<>();
    private final ObjectMapper objectMapper;
    private final ResourceMonitorService resourceMonitorService;
    private ScheduledExecutorService scheduler;
    private ProcessorContext context;
    private final MeterRegistry meterRegistry;


    // Micrometer meters
    private Counter totalBatches, totalMessages, parseFailures;
    private DistributionSummary batchSizeSummary;
    private Timer flushTimer;
    private final AtomicLong lastBatchSizeGauge = new AtomicLong(0);
    private final AtomicLong lastBatchEpochSecondsGauge = new AtomicLong(0);
    private final AtomicLong bufferSizeGauge = new AtomicLong(0);


    public BatchedDbWriterTransformer(ObjectMapper objectMapper,
                                      ResourceMonitorService resourceMonitorService,
                                      MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.resourceMonitorService = resourceMonitorService;
        this.meterRegistry = meterRegistry;
    }

    // 스케줄러 설정
    @Override
    public void init(ProcessorContext context) {
        this.context = context;

        // 1) 여기서 등록 + taskId 태그로 충돌 방지
        String taskId = context.taskId().toString();
        Tags tags = Tags.of("component","db-writer","task",taskId);

        this.totalBatches = meterRegistry.find("resource_batch_total").tags(tags).counter();
        if (this.totalBatches == null) {
            this.totalBatches = Counter.builder("resource_batch_total").tags(tags).register(meterRegistry);
        }
        this.totalMessages = meterRegistry.find("resource_messages_total").tags(tags).counter();
        if (this.totalMessages == null) {
            this.totalMessages = Counter.builder("resource_messages_total").tags(tags).register(meterRegistry);
        }
        this.parseFailures = meterRegistry.find("resource_parse_failures_total").tags(tags).counter();
        if (this.parseFailures == null) {
            this.parseFailures = Counter.builder("resource_parse_failures_total").tags(tags).register(meterRegistry);
        }
        this.batchSizeSummary = meterRegistry.find("resource_batch_size").tags(tags).summary();
        if (this.batchSizeSummary == null) {
            this.batchSizeSummary = DistributionSummary.builder("resource_batch_size")
                    .baseUnit("messages").publishPercentileHistogram().tags(tags).register(meterRegistry);
        }
        this.flushTimer = meterRegistry.find("resource_batch_flush_duration").tags(tags).timer();
        if (this.flushTimer == null) {
            this.flushTimer = Timer.builder("resource_batch_flush_duration")
                    .publishPercentileHistogram().tags(tags).register(meterRegistry);
        }
        if (meterRegistry.find("resource_last_batch_size").tags(tags).gauge() == null) {
            Gauge.builder("resource_last_batch_size", lastBatchSizeGauge, AtomicLong::get)
                    .tags(tags).register(meterRegistry);
        }
        if (meterRegistry.find("resource_last_batch_time_epoch").tags(tags).gauge() == null) {
            Gauge.builder("resource_last_batch_time_epoch", lastBatchEpochSecondsGauge, AtomicLong::get)
                    .baseUnit("seconds").tags(tags).register(meterRegistry);
        }
        if (meterRegistry.find("resource_buffer_size").tags(tags).gauge() == null) {
            Gauge.builder("resource_buffer_size", bufferSizeGauge, AtomicLong::get)
                    .baseUnit("messages").tags(tags).register(meterRegistry);
        }

        startScheduler();
    }

    // 메시지 파싱 및 버퍼 추가
    @Override
    public KeyValue<String, String> transform(String key, String value) {
        try {
            KafkaCommonMessage<SystemResourceMetricsMessage> parsed = objectMapper.readValue(value, new TypeReference<>() {});
            addToBuffer(parsed);
        } catch (Exception e) {
            if (parseFailures != null) parseFailures.increment();
            log.error("[BatchedDbWriterTransformer] JSON 파싱 실패. value: {}, error: {}", value, e.getMessage());
        }
        return null;
    }

    @Override
    public void close() {
        safeFlushBuffer();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private void flushBuffer() {
        List<KafkaCommonMessage<SystemResourceMetricsMessage>> toSave;
        synchronized (buffer) {
            if (buffer.isEmpty()) return;
            toSave = new ArrayList<>(buffer);
            buffer.clear();
            bufferSizeGauge.set(0);
        }

        flushTimer.record(() -> {
            try {
                resourceMonitorService.saveSystemResourceMetricsBulk(toSave);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        int size = toSave.size();
        totalBatches.increment();
        totalMessages.increment(size);
        batchSizeSummary.record(size);
        lastBatchSizeGauge.set(size);
        lastBatchEpochSecondsGauge.set(Instant.now().getEpochSecond());
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
        boolean shouldFlush = false;
        synchronized (buffer) {
            buffer.add(message);
            bufferSizeGauge.set(buffer.size());
            if (buffer.size() >= batchSize) {
                shouldFlush = true; // 락 밖에서 flush
            }
        }
        if (shouldFlush) {
            safeFlushBuffer();
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
