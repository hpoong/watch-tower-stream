package com.hopoong.resource.usecase.resourcemonitor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.core.topic.KafkaTopicManager;
import com.hopoong.core.util.TimeUtil;
import com.hopoong.resource.api.resourcemonitor.model.ResourceMonitorRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceMonitorServiceImpl implements ResourceMonitorService {

    private final ConsumerFactory<String, Object> consumerFactory;
    private final ProducerFactory<String, Object> producerFactory;
    private final ObjectMapper objectMapper;

    private static final double STAGE1_THRESHOLD = 70.0;
    private static final double STAGE2_THRESHOLD = 80.0;
    private static final double STAGE3_THRESHOLD = 90.0;

    // 시간 기반 시드를 생성하는 메서드
    private long getTimeBasedSeed() {
        LocalDateTime now = LocalDateTime.now();
        // 분 단위로 시드 생성 (같은 분에는 비슷한 값)
        return now.getYear() * 100000000L + 
               now.getMonthValue() * 1000000L + 
               now.getDayOfMonth() * 10000L + 
               now.getHour() * 100L + 
               now.getMinute();
    }

    // 시간대별 CPU 사용량 패턴 (업무시간에 높음)
    private double getCpuUsageByTime() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        
        // 업무시간 (9-18)에는 높은 사용량, 야간에는 낮은 사용량
        double baseUsage;
        if (hour >= 9 && hour <=18) {
            // 업무시간: 40% ~ 60%
            baseUsage =40.0 + (hour - 9) * 20.0; // 시간이 지날수록 점점 증가
        } else if (hour >=19 && hour <=22) {
            // 저녁시간: 30% ~ 20%
            baseUsage = 30.0 - (hour - 19) * 20.5;
        } else {
            // 야간시간: 10% ~ 25%
            baseUsage =10.0 + (hour %6 * 2.5);
        }
        
        return baseUsage;
    }

    // 시간대별 메모리 사용량 패턴
    private double getMemoryUsageByTime() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        
        // 메모리는 CPU보다 안정적이지만 업무시간에 약간 높음
        double baseUsage;
        if (hour >= 9 && hour <=18) {
            // 업무시간: 60% ~ 70%
            baseUsage =60.0 + (hour -9) * 10.0;
        } else {
            // 비업무시간: 45% ~ 55%
            baseUsage =45.0 + (hour % 8* 1.25);
        }
        
        return baseUsage;
    }

    // 시간대별 디스크 사용량 패턴 (점진적 증가)
    private double getDiskUsageByTime() {
        LocalDateTime now = LocalDateTime.now();
        int dayOfYear = now.getDayOfYear();
        
        // 디스크는 점진적으로 증가하는 패턴
        double baseUsage =500.0 + (dayOfYear %30) * 0.5; // 30일 주기로 0.5가
        
        // 시간대별 변동 추가
        LocalTime time = now.toLocalTime();
        int hour = time.getHour();
        if (hour >= 9 && hour <=18) {         // 업무시간에 약간 증가
            baseUsage += 2.0;
        }
        
        return Math.min(baseUsage, 95.0); // 최대 95로 제한
    }

    // CPU
    @Override
    public double measureCpuUsage() {
        long seed = getTimeBasedSeed();
        Random random = new Random(seed);
        
        double baseUsage = getCpuUsageByTime();
        double variation = random.nextDouble(-100, 15.0); // -10% ~ +15% 변동
        
        double result = baseUsage + variation;
        return roundToThreeDecimalPlaces(Math.max(0.0, Math.min(1000, result)));
    }

    // Memory
    @Override
    public double measureMemoryUsage() {
        long seed = getTimeBasedSeed();
        Random random = new Random(seed);
        
        double baseUsage = getMemoryUsageByTime();
        double variation = random.nextDouble(-50, 80); // -5% ~ +8% 변동
        
        double result = baseUsage + variation;
        return roundToThreeDecimalPlaces(Math.max(0.0, Math.min(1000, result)));
    }

    // Disk
    @Override
    public double measureDiskUsage() {
        long seed = getTimeBasedSeed();
        Random random = new Random(seed);
        
        double baseUsage = getDiskUsageByTime();
        double variation = random.nextDouble(-20, 30); // -2% ~ +3% 변동 (디스크는 안정적)
        
        double result = baseUsage + variation;
        return roundToThreeDecimalPlaces(Math.max(0.0, Math.min(1000, result)));
    }

    public double roundToThreeDecimalPlaces(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    @Override
    public String determineAlert(double usage) {
        return usage >= STAGE3_THRESHOLD ? "critical" :
            usage >= STAGE2_THRESHOLD ? "warning" :
            usage >= STAGE1_THRESHOLD ? "info" : "normal";
    }

    @Override
    public void replayServerMetrics(ResourceMonitorRequest request) {
        String topic = request.topic() != null ? request.topic() : KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC;

        long startMillis = TimeUtil.toMillis(request.startTime());
        long endMillis = TimeUtil.toMillis(request.endTime());

        try (
                Consumer<String, Object> consumer = consumerFactory.createConsumer();
                Producer<String, Object> producer = producerFactory.createProducer()
        ) {

            List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
            List<TopicPartition> topicPartitions = partitionInfos.stream()
                    .map(p -> new TopicPartition(topic, p.partition()))
                    .collect(Collectors.toList());

            consumer.assign(topicPartitions);

            Map<TopicPartition, Long> timestampsToSearch = topicPartitions.stream()
                    .collect(Collectors.toMap(Function.identity(), tp -> startMillis));

            Map<TopicPartition, OffsetAndTimestamp> offsetsForTimes = consumer.offsetsForTimes(timestampsToSearch);

            for (TopicPartition tp : topicPartitions) {
                OffsetAndTimestamp offsetAndTimestamp = offsetsForTimes.get(tp);
                if (offsetAndTimestamp != null) {
                    consumer.seek(tp, offsetAndTimestamp.offset());
                }
            }

            boolean done = false;
            while (!done) {
                ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(1));
                if (records.isEmpty()) {
                    break;
                }

                for (ConsumerRecord<String, Object> record : records) {
                    if (record.timestamp() > endMillis) {
                        done = true;
                        break;
                    }

                    JsonNode jsonNode = objectMapper.readTree(record.value().toString());
                    JsonNode bodyNode = jsonNode.get("body");
                    String serverName = bodyNode.get("serverName").asText();
                    String resourceName = bodyNode.get("resourceName").asText();

                    if (serverName.equals(request.serverName()) && resourceName.equals(request.resourceName())) {
                        producer.send(new ProducerRecord<>(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_REPLAY_TOPIC, record.key(), record.value()));
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Replay 실패", e);
        }
    }
}
