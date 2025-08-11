package com.hopoong.core;

import com.hopoong.core.index.ElasticsearchIndexProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@EnableConfigurationProperties(ElasticsearchIndexProperties.class)
public class CoreConfig implements CommandLineRunner {

    private final Environment env;

    public CoreConfig(Environment env) {
        this.env = env;
    }

    @Override
    public void run(String... args) {
        System.out.println("==== application-core.yml direct read test ====");
        System.out.println("systemMetrics => " + env.getProperty("elasticsearch.index.system-metrics"));
        System.out.println("systemMetrics5Min => " + env.getProperty("elasticsearch.index.system-metrics-5min"));
        System.out.println("==========================================");
    }
}