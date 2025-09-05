package com.hopoong.audit.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
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
    private Long id; // BIGSERIAL

    @Column(name = "aggregate_type", length = 64, nullable = false)
    private String aggregateType; // user_event, user_security_event

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "topic", length = 128, nullable = false)
    private String topic; // 예: user.action.v1

    @Column(name = "record_key", length = 256, nullable = false)
    private String recordKey; // Kafka record key (userId 또는 userId:sessionId)

    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload; // Avro/Proto 직렬화 전 논리 페이로드(로깅/리플레이 용)

    @Column(name = "headers", columnDefinition = "jsonb")
    private String headers; // nullable

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt; // DB default now()

    @Column(name = "published_at")
    private OffsetDateTime publishedAt; // 전송 완료 시각

    @Column(name = "error_count", nullable = false)
    private Integer errorCount = 0; // DEFAULT 0

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}
