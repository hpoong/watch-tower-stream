package com.hopoong.core.index;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "elasticsearch.index")
public class ElasticsearchIndexProperties {
    private String systemMetrics;
    private String systemMetrics5Min;
}