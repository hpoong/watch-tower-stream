package com.hopoong.audit.usecase.user.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.repository.OutboxEventBatchDao;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OutboxRelayService {

    private static final int BATCH_SIZE = 200;
    private static final int MAX_RETRY = 10; // 재시도 횟수
    private static final String DLT_TOPIC = "user.dlt.v1";

    private final OutboxEventBatchDao outboxDao;
    private final AvroMapper avroMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper om;
    private final JdbcTemplate jdbc;


    // 고정 지연 또는 크론 스케줄링. 운영에서는 메트릭 기반 가변화 가능.
    @Scheduled(fixedDelayString = "2000") // 2초마다
    @Transactional
    public void relay() {
        // 배치 잠금
        var batch = outboxDao.lockNextBatch(BATCH_SIZE, MAX_RETRY);
        if (batch.isEmpty()) return;

        for (var row : batch) {
            try {

                // 헤더에서 schema 이름/traceId 등 꺼내기
                JsonNode headers = om.readTree(row.headersJson());
                String schema = String.valueOf(headers.has("schema"));
                String traceId = String.valueOf(headers.has("traceId"));

                // JSON → Avro SpecificRecord
                SpecificRecord avro = avroMapper.toSpecific(schema, row.payloadJson());

                // Kafka publish (키는 outbox.record_key)
                var record = new ProducerRecord<String, Object>(row.topic(), row.recordKey(), avro);

                // Kafka 헤더 부가
                if (traceId != null) {
                    record.headers().add(new RecordHeader("traceId", traceId.getBytes(StandardCharsets.UTF_8)));
                }
                record.headers().add(new RecordHeader("schema", schema.getBytes(StandardCharsets.UTF_8)));

                // 토픽 전송
                kafkaTemplate.send(record).get(); // 동기 전송(간단). 고성능이면 콜백 + 트랜잭션 경계 조정

                // 성공 마킹
                outboxDao.markPublished(row.id(), OffsetDateTime.now(ZoneOffset.UTC));

            } catch (Exception e) {
                // 실패 마킹
                outboxDao.markFailed(row.id(), e.getClass().getSimpleName() + ": " + e.getMessage());

                // 재시도 임계 초과 → DLT 저장 + DLT 토픽 발행
                handleIfExceeded(row, e);
            }
        }
    }

    private void handleIfExceeded(OutboxEventBatchDao.OutboxRow row, Exception e) {
        // 현재 카운트 확인
        Integer ec = jdbc.queryForObject("SELECT error_count FROM um.outbox_event WHERE id = ?",
                Integer.class, row.id());
        if (ec != null && ec >= MAX_RETRY) {
            // dlt_store 기록
            outboxDao.insertDlt(row.id(), row.topic(), row.tenantId(), row.recordKey(),
                    row.payloadJson(), row.headersJson(), e.getMessage());

            // DLT 토픽 발행 (원문 그대로 래핑하여 알림)
            Map<String, Object> dltPayload = Map.of(
                    "sourceOutboxId", row.id(),
                    "sourceTopic", row.topic(),
                    "tenantId", row.tenantId(),
                    "recordKey", row.recordKey(),
                    "payload", safeJson(row.payloadJson()),
                    "headers", safeJson(row.headersJson()),
                    "reason", e.getMessage()
            );

            var rec = new ProducerRecord<String, Object>(DLT_TOPIC, row.recordKey(), dltPayload);
            kafkaTemplate.send(rec); // DLT는 Avro가 아니어도 되지만 운영 포맷 통일 권장
        }
    }


    private Object safeJson(String json) {
        try { return om.readTree(json); } catch (Exception e) { return Map.of("raw", json); }
    }


}
