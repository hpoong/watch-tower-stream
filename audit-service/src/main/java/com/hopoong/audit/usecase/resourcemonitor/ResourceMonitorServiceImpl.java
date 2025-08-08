package com.hopoong.audit.usecase.resourcemonitor;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hopoong.audit.adapter.kafka.resourcemetric.model.AvgMax;
import com.hopoong.audit.persistence.document.StatisticalMetricDocument;
import com.hopoong.audit.persistence.document.SystemMetricDocument;
import com.hopoong.core.index.ElasticsearchIndexProperties;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsErrorMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceMonitorServiceImpl implements ResourceMonitorService {

    private final ElasticsearchClient elasticsearchClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ElasticsearchIndexProperties elasticsearchIndexProperties;

    private static final String TRACE_ID_CACHE_PREFIX = "trace_id:";
    private static final Duration TRACE_ID_CACHE_TTL = Duration.ofHours(24);


    @Override
    public void saveSystemResourceMetrics(KafkaCommonMessage<SystemResourceMetricsMessage> message) throws IOException {
        SystemMetricDocument document = SystemMetricDocument.toSystemMetricDocument(message);

        elasticsearchClient.index(IndexRequest.of(i -> i
                .index(elasticsearchIndexProperties.getSystemMetrics())
                .document(document)
        ));

        String cacheKey = TRACE_ID_CACHE_PREFIX + document.getTraceId();
        setCacheExistenceFlag(cacheKey, true);
    }

    @Override
    public boolean existsByTraceId(String traceId) throws IOException {
        String cacheKey = TRACE_ID_CACHE_PREFIX + traceId;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return "1".equals(cached.toString()); // "1"이면 true, "0"이면 false
        }

        long count = elasticsearchClient.count(c -> c
                .index(elasticsearchIndexProperties.getSystemMetrics())
                .query(q -> q.term(t -> t.field("traceId").value(traceId)))
        ).count();

        boolean exists = count > 0;
        setCacheExistenceFlag(cacheKey, exists);
        return exists;
    }

    private void setCacheExistenceFlag(String cacheKey, boolean exists) {
        redisTemplate.opsForValue().set(
                cacheKey,
                exists ? "1" : "0",
                exists ? TRACE_ID_CACHE_TTL : Duration.ofMinutes(15)
        );
    }

    @Override
    public void saveSystemResourceMetricsBulk(List<KafkaCommonMessage<SystemResourceMetricsMessage>> messages) throws IOException {
        List<List<KafkaCommonMessage<SystemResourceMetricsMessage>>> partitions = Lists.partition(messages, 1000);

        for (List<KafkaCommonMessage<SystemResourceMetricsMessage>> batch : partitions) {
            List<BulkOperation> operations = new ArrayList<>();

            for (KafkaCommonMessage<SystemResourceMetricsMessage> message : batch) {
                SystemMetricDocument document = SystemMetricDocument.toSystemMetricDocument(message);

                operations.add(BulkOperation.of(op -> op
                        .index(idx -> idx
                                .index(elasticsearchIndexProperties.getSystemMetrics())
                                .document(document)
                        )
                ));
            }

            BulkRequest request = new BulkRequest.Builder()
                    .operations(operations)
                    .build();

            BulkResponse response = elasticsearchClient.bulk(request);

            if (response.errors()) {
                List<BulkResponseItem> items = response.items();
                for (int i = 0; i < items.size(); i++) {
                    BulkResponseItem item = items.get(i);

                    if (item.error() != null) {
                        KafkaCommonMessage<SystemResourceMetricsMessage> originalMessage = batch.get(i);

                        SystemResourceMetricsErrorMessage systemResourceMetricsErrorMessage = SystemResourceMetricsErrorMessage.builder()
                                .orgMessage(originalMessage.getBody())
                                .errorType("INSERT")
                                .build();

                        KafkaCommonMessage<?> kafkaMessage = KafkaCommonMessage.builder()
                                .header(originalMessage.getHeader())
                                .body(systemResourceMetricsErrorMessage)
                                .build();

                        String payload = objectMapper.writeValueAsString(kafkaMessage);
                        kafkaTemplate.send(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_ERROR_TOPIC, payload);
                    }
                }
            }
        }
    }

    @Override
    public void saveSystemResourceMetrics5Min(AvgMax value) throws IOException {
        StatisticalMetricDocument document = StatisticalMetricDocument.toStatisticalMetricDocument(value);

        elasticsearchClient.index(IndexRequest.of(i -> i
                .index(elasticsearchIndexProperties.getSystemMetrics5Min())
                .document(document)
        ));
    }

}
