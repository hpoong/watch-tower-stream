package com.hopoong.audit.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;


/* =========================
   outbox_event
   PK: id (BIGSERIAL)
   ========================= */
@Entity
@Table(name = "outbox_event", schema = "um")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "aggregate_type", length = 64, nullable = false)
    private String aggregateType; // 이벤트 원본 도메인

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "topic", length = 128, nullable = false)
    private String topic; // 발행할 Kafka 토픽

    @Column(name = "record_key", length = 256, nullable = false)
    private String recordKey; // Kafka record key (userId 또는 userId:sessionId)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private JsonNode payload; // 원본 도메인 메시지 데이터

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", columnDefinition = "jsonb")
    private JsonNode headers;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt; // 전송 완료 시각

    @Column(name = "error_count", nullable = false)
    private Integer errorCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError; // 실패 사유
}
