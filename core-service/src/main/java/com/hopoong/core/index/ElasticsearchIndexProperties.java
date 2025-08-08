package com.hopoong.core.index;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "elasticsearch.index")
public class ElasticsearchIndexProperties {
    private String systemMetrics;
    private String systemMetrics5Min;
}