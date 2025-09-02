 package com.hopoong.audit.api;

import com.hopoong.core.topic.KafkaTopicManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;


@Service
@RequiredArgsConstructor
public class TopicRemoveService {

    // 필요 시 yml로 뺄 수 있습니다.
    private static final String BASE_URL = "http://localhost:7081";
    private static final String CLUSTER  = "Kafka-Cluster";

    public static final String[] TOPICS = new String[] {
            KafkaTopicManager.SYSTEM_RESOURCE_METRICS_TOPIC,
            KafkaTopicManager.SYSTEM_RESOURCE_METRICS_REPROCESS_TOPIC,
            KafkaTopicManager.SYSTEM_RESOURCE_METRICS_DLQ_TOPIC,
            KafkaTopicManager.SYSTEM_RESOURCE_METRICS_ERROR_TOPIC,
            KafkaTopicManager.SYSTEM_RESOURCE_METRICS_REPLAY_TOPIC,
            KafkaTopicManager.SYSTEM_THRESHOLD_TOPIC,
            KafkaTopicManager.USER_ACTION_EVENTS,
            KafkaTopicManager.USER_FEATURE_COUNT_5M
    };

    private final RestTemplate rest = new RestTemplate();

    /**
     * 전부 비우기(일괄). 실패 시 RuntimeException 발생.
     */
    public void purgeAll() {
        for (String topic : TOPICS) {
            String url = BASE_URL + "/api/clusters/" + CLUSTER + "/topics/" + topic + "/messages";
            try {
                rest.exchange(new RequestEntity<>(HttpMethod.DELETE, URI.create(url)), Void.class);
            } catch (Exception e) {
                throw new RuntimeException("Purge failed for topic=" + topic + " (" + url + "): " + e.getMessage(), e);
            }
        }
    }





}

