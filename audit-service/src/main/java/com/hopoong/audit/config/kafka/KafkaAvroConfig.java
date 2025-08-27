package com.hopoong.audit.config.kafka;

import com.hopoong.audit.common.exception.KafkaProcessingException;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaAvroConfig {

    private final KafkaProperties kafkaProperties;

    @Value("${app.kafka.avro.group-id:user-event-consumer-group}")
    private String avroGroupId;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${spring.kafka.properties.auto.register.schemas:true}")
    private boolean autoRegisterSchemas;

    @Value("${spring.kafka.properties.specific.avro.reader:true}")
    private boolean specificAvroReader;

    @Bean(name = "avroProducerFactory")
    public ProducerFactory<String, SpecificRecord> avroProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

        // Schema Registry 설정
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("auto.register.schemas", autoRegisterSchemas);

        String acks = kafkaProperties.getProducer().getAcks();
        props.put(ProducerConfig.ACKS_CONFIG, (acks != null ? acks : "1"));

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean(name = "avroKafkaTemplate")
    public KafkaTemplate<String, SpecificRecord> avroKafkaTemplate() {
        return new KafkaTemplate<>(avroProducerFactory());
    }

    @Bean(name = "avroConsumerFactory")
    public ConsumerFactory<String, SpecificRecord> avroConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, avroGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);

        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", specificAvroReader);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean(name = "avroKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, SpecificRecord> avroKafkaListenerContainerFactory(
            ConsumerFactory<String, SpecificRecord> avroConsumerFactory,
            DefaultErrorHandler avroKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, SpecificRecord> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(avroConsumerFactory);
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(avroKafkaErrorHandler);
        return factory;
    }

    @Bean(name = "avroKafkaErrorHandler")
    public DefaultErrorHandler avroKafkaErrorHandler(
            KafkaTemplate<String, SpecificRecord> avroKafkaTemplate
    ) {
        ConsumerRecordRecoverer recover = (record, ex) -> {
            try {
                avroKafkaTemplate
                    .send(record.topic() + ".DLQ", String.valueOf(record.key()), (SpecificRecord) record.value()).get();
            } catch (Exception sendEx) {
                throw new KafkaException("Avro DLQ 전송 실패", sendEx);
            }
        };

        DefaultErrorHandler handler = new DefaultErrorHandler(recover, new org.springframework.util.backoff.FixedBackOff(0, 0));
         handler.addRetryableExceptions(KafkaProcessingException.class);
        return handler;
    }
}
