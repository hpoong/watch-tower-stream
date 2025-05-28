package com.hopoong.audit.usecase.resourcemonitor;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.InlineGet;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hopoong.audit.persistence.document.SystemMetricDocument;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceMonitorServiceImpl implements ResourceMonitorService {

    private final ElasticsearchClient elasticsearchClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void insertSystemResourceMetrics(KafkaCommonMessage<SystemResourceMetricsMessage> message) throws IOException {

        SystemMetricDocument metric = SystemMetricDocument.builder()
                .resourceName(message.getBody().resourceName())
                .usagePercent(message.getBody().usagePercent())
                .alertLevel(message.getBody().alertLevel())
                .serverName(message.getBody().serverName())
                .ipAddress(message.getBody().ipAddress())
                .timestamp(message.getHeader().getTimestamp())
                .traceId(message.getHeader().getTraceId())
                .build();

        elasticsearchClient.index(IndexRequest.of(i -> i
                .index("system_metrics")
                .document(metric)
        ));

    }

    @Override
    public boolean existsByTraceId(String traceId) throws IOException {
        SearchResponse<Map> response = elasticsearchClient.search(s -> s
                .index("system_metrics")
                .query(q -> q.term(t -> t.field("traceId").value(traceId))),
                Map.class
        );

        return response.hits().total().value() > 0;
    }

    @Override
    public void insertSystemResourceMetricsBulk(List<KafkaCommonMessage<SystemResourceMetricsMessage>> messages) throws IOException {
        List<List<KafkaCommonMessage<SystemResourceMetricsMessage>>> partitions = Lists.partition(messages, 1000);

        for (List<KafkaCommonMessage<SystemResourceMetricsMessage>> batch : partitions) {
            List<BulkOperation> operations = new ArrayList<>();

            for (KafkaCommonMessage<SystemResourceMetricsMessage> message : batch) {
                SystemMetricDocument metric = SystemMetricDocument.builder()
                        .resourceName(message.getBody().resourceName())
                        .usagePercent(message.getBody().usagePercent())
                        .alertLevel(message.getBody().alertLevel())
                        .serverName(message.getBody().serverName())
                        .ipAddress(message.getBody().ipAddress())
                        .timestamp(message.getHeader().getTimestamp())
                        .traceId(message.getHeader().getTraceId())
                        .build();

                operations.add(BulkOperation.of(op -> op
                        .index(idx -> idx
                                .index("system_metrics")
                                .document(metric)
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
                        String payload = objectMapper.writeValueAsString(originalMessage);
                        kafkaTemplate.send(KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC + ".ERROR", payload);
                    }
                }
            }
        }
    }



}
