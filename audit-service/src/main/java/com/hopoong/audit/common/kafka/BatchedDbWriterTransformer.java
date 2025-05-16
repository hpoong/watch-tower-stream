package com.hopoong.audit.common.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.core.message.common.KafkaCommonMessage;
import com.hopoong.core.message.resourcemonitor.SystemResourceMetricsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BatchedDbWriterTransformer implements Transformer<String, String, KeyValue<String, String>> {

    private final int batchSize;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<SystemResourceMetricsMessage> buffer = new ArrayList<>();

    @Override
    public void init(ProcessorContext context) {
        // 상태 저장 필요 없음
    }

    @Override
    public KeyValue<String, String> transform(String key, String value) {
        try {
            // 메시지 파싱
            KafkaCommonMessage<SystemResourceMetricsMessage> message =
                    objectMapper.readValue(value, new TypeReference<>() {});

            buffer.add(message.getBody());

            if (buffer.size() >= batchSize) {
                // DB 저장 로직 호출
                saveToDatabase(buffer);
                buffer.clear();
            }

        } catch (Exception e) {
            log.error("메시지 파싱 또는 DB 저장 중 오류 발생: {}", e.getMessage(), e);
        }

        return null; // 출력 없음
    }

    @Override
    public void close() {
        if (!buffer.isEmpty()) {
            try {
                saveToDatabase(buffer);
                buffer.clear();
            } catch (Exception e) {
                log.error("close() 중 잔여 데이터 저장 실패: {}", e.getMessage(), e);
            }
        }
    }

    private void saveToDatabase(List<SystemResourceMetricsMessage> messages) {
        log.info("DB에 저장: {}건", messages.size());
        for (SystemResourceMetricsMessage msg : messages) {
            log.info("  ↳ {} {}% [{}] @ {}", msg.resourceName(), msg.usagePercent(), msg.alertLevel(), msg.serverName());
        }
    }
}
