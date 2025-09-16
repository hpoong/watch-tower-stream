package com.hopoong.core.topic;

public class KafkaTopicManager {
    public static final String SYSTEM_RESOURCE_METRICS_TOPIC = "system-resource-metrics";
    public static final String SYSTEM_RESOURCE_METRICS_REPROCESS_TOPIC = "system-resource-metrics.REPROCESS";
    public static final String SYSTEM_RESOURCE_METRICS_DLQ_TOPIC = "system-resource-metrics.DLQ";
    public static final String SYSTEM_RESOURCE_METRICS_ERROR_TOPIC = "system-resource-metrics.ERROR";
    public static final String SYSTEM_RESOURCE_METRICS_REPLAY_TOPIC = "system-resource-metrics.REPLAY";


    public static final String SYSTEM_THRESHOLD_TOPIC = "system-threshold";


    public static final String USER_ACTION_EVENTS = "user-action-events";
    public static final String USER_ACTION_EVENTS_SHADOW = "user-action-events.shadow";
    public static final String USER_ACTION_EVENTS_ENRICHED_V1 = "user-action-events-enriched.v1";
    public static final String USER_FEATURE_COUNT_5M = "user-feature-count-5m";
}
