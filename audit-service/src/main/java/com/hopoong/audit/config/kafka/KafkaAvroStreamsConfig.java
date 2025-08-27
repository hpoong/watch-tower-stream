package com.hopoong.audit.config.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableKafkaStreams
@RequiredArgsConstructor
public class KafkaAvroStreamsConfig {

    private final KafkaProperties kafkaProperties;

    @Value("${app.kafka.avro-streams.application-id:audit-service-avro-dev}")
    private String avroStreamsAppId;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${spring.kafka.properties.auto.register.schemas:true}")
    private boolean autoRegisterSchemas;

    @Value("${spring.kafka.properties.specific.avro.reader:true}")
    private boolean specificAvroReader;


    @Bean(name = "avroStreamsConfig")
    public KafkaStreamsConfiguration avroStreamsConfiguration() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, avroStreamsAppId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class.getName());

        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("auto.register.schemas", autoRegisterSchemas);
        props.put("specific.avro.reader", specificAvroReader);

        return new KafkaStreamsConfiguration(props);
    }

    @Bean(name = "avroStreamsBuilderFactoryBean")
    public StreamsBuilderFactoryBean avroStreamsBuilderFactoryBean() {
        return new StreamsBuilderFactoryBean(avroStreamsConfiguration());
    }
}