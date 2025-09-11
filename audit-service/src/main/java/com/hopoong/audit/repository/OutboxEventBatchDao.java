package com.hopoong.audit.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxEventBatchDao {

    private final JdbcTemplate jdbc;

    public List<OutboxRow> lockNextBatch(int limit, int maxRetry) {
        // 필요 시 백오프 조건 추가: AND (last_attempt_at IS NULL OR last_attempt_at <= now() - interval '...')
        String sql = """
            SELECT id, aggregate_type, aggregate_id, tenant_id, topic, record_key, payload::text AS payload, headers::text AS headers
            FROM um.outbox_event
            WHERE published_at IS NULL
              AND error_count < ?
            ORDER BY id
            FOR UPDATE SKIP LOCKED
            LIMIT ?
            """;
        return jdbc.query(sql, (rs, rn) -> new OutboxRow(
                rs.getLong("id"),
                rs.getString("aggregate_type"),
                UUID.fromString(rs.getString("aggregate_id")),
                rs.getString("tenant_id"),
                rs.getString("topic"),
                rs.getString("record_key"),
                rs.getString("payload"),
                rs.getString("headers")
        ), maxRetry, limit);
    }

    public void markPublished(long id, OffsetDateTime ts) {
        jdbc.update("UPDATE um.outbox_event SET published_at = ?, last_error = NULL WHERE id = ?", ts, id);
    }

    public void markFailed(long id, String errMsg) {
        jdbc.update("""
            UPDATE um.outbox_event 
               SET error_count = error_count + 1, last_error = ?, published_at = NULL
             WHERE id = ?
        """, truncate(errMsg, 2000), id);
    }

    public void insertDlt(long outboxId, String sourceTopic, String tenantId,
                          String recordKey, String payload, String headers, String reason) {
        jdbc.update("""
            INSERT INTO um.dlt_store (source_topic, tenant_id, record_key, payload, headers, reason)
            VALUES (?,?,?,?,?::jsonb,?)
        """, sourceTopic, tenantId, recordKey, payload, headers, reason);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    public record OutboxRow(
            long id,
            String aggregateType,
            UUID aggregateId,
            String tenantId,
            String topic,
            String recordKey,
            String payloadJson,   // JSON 문자열
            String headersJson    // JSON 문자열
    ) {}
}