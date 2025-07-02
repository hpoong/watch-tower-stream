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
import java.util.List;
import java.util.Map;
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

    // CPU
    @Override
    public double measureCpuUsage() {
        double raw = ThreadLocalRandom.current().nextDouble(0.0, 1.0);
        return roundToThreeDecimalPlaces(raw * 100);
    }

    // Memory
    @Override
    public double measureMemoryUsage() {
        long total = 16L * 1024 * 1024 * 1024;
        long free = ThreadLocalRandom.current().nextLong(0, total);
        double usage = (total - free) / (double) total;
        return roundToThreeDecimalPlaces(usage * 100);
    }

    // Disk
    @Override
    public double measureDiskUsage() {
        long total = 500L * 1024 * 1024 * 1024;
        long usable = ThreadLocalRandom.current().nextLong(0, total);
        double usage = (total - usable) / (double) total;
        return roundToThreeDecimalPlaces(usage * 100);
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
