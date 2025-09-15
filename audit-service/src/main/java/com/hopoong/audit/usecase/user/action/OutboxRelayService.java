package com.hopoong.audit.usecase.user.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.hopoong.audit.persistence.entity.DltStoreEntity;
import com.hopoong.audit.persistence.entity.OutboxEventEntity;
import com.hopoong.audit.repository.DltStoreJpaRepository;
import com.hopoong.audit.repository.OutboxEventJpaRepository;
import com.hopoong.core.util.AvroJsonUtil;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxRelayService {

    private static final int BATCH_SIZE = 200;
    private static final int MAX_RETRY = 10; // 재시도 횟수

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final DltStoreJpaRepository dltStoreJpaRepository;


    // 2초마다 배치 처리
    @Scheduled(fixedDelayString = "2000")
    @Transactional
    public void relay() {
        List<OutboxEventEntity> batch = outboxEventJpaRepository.lockBatchForPublish(BATCH_SIZE, MAX_RETRY);

        // 배치 잠금
        if (batch.isEmpty()) return;

        for (var row : batch) {
            try {
                JsonNode headers = row.getHeaders();
                String schema = String.valueOf(headers.has("schema"));
                String traceId = String.valueOf(headers.has("traceId"));

                // JSON → Avro SpecificRecord
                SpecificRecord avro = AvroJsonUtil.toAvroRecord(schema, row.getPayload());

                // Kafka publish
                var record = new ProducerRecord<String, Object>(row.getTopic(), row.getRecordKey(), avro);

                // Kafka header
                record.headers().add(new RecordHeader("traceId", traceId.getBytes(StandardCharsets.UTF_8)));
                record.headers().add(new RecordHeader("schema", schema.getBytes(StandardCharsets.UTF_8)));

                // 토픽 전송
                kafkaTemplate.send(record).get(); // 동기 전송

                // 성공 마킹
                outboxEventJpaRepository.markPublished(row.getId(), OffsetDateTime.now(ZoneOffset.UTC));

            } catch (Exception e) {
                // 실패 마킹
                outboxEventJpaRepository.markFailed(row.getId(), e.getClass().getSimpleName() + ": " + e.getMessage());

                // 재시도 임계 초과 → DLT 저장 + DLT 토픽 발행
                handleIfExceeded(row, e);
            }
        }
    }

    private void handleIfExceeded(OutboxEventEntity row, Exception e) {

        if (row.getErrorCount() >= MAX_RETRY) {

            DltStoreEntity dlt = DltStoreEntity.builder()
                    .sourceTopic(row.getTopic())
                    .tenantId(row.getTenantId())
                    .recordKey(row.getRecordKey())
                    .payload(row.getPayload())   // 원본 payload 그대로
                    .headers(row.getHeaders())
                    .reason(e.getMessage())
                    .failedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();

            dltStoreJpaRepository.save(dlt);
        }
    }

}
