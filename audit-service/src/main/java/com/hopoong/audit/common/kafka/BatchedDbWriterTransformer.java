package com.hopoong.audit.common.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class BatchedDbWriterTransformer implements Transformer<String, String, KeyValue<String, String>> {

    private final int batchSize;
    private final List<SystemResourceMetricsMessage> buffer;
    private final ObjectMapper objectMapper;

    public BatchedDbWriterTransformer(int batchSize) {
        this.batchSize = batchSize;
        this.buffer = new ArrayList<>();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void init(ProcessorContext context) {
        // 상태 초기화 필요 시 여기에
    }

    @Override
    public KeyValue<String, String> transform(String key, String value) {
        try {
            KafkaCommonMessage<SystemResourceMetricsMessage> message =
                    objectMapper.readValue(value, new TypeReference<>() {});

            buffer.add(message.getBody());

            if (buffer.size() >= batchSize) {
                insertBatchToDatabase(buffer);
                buffer.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void insertBatchToDatabase(List<SystemResourceMetricsMessage> messages) {
        System.out.println("DB 저장 : " + messages.size() + "건");
    }

    @Override
    public void close() {
        if (!buffer.isEmpty()) {
            insertBatchToDatabase(buffer);
        }
    }
}
