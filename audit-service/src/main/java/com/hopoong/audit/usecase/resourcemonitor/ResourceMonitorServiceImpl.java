package com.hopoong.audit.usecase.resourcemonitor;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.hopoong.audit.persistence.document.SystemMetricDocument;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceMonitorServiceImpl implements ResourceMonitorService {

    private final ElasticsearchClient elasticsearchClient;

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

}
