package com.hopoong.audit.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/* =========================
   dlt_store
   PK: id (BIGSERIAL)
   ========================= */
@Entity
@Table(name = "dlt_store", schema = "um")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DltStoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id; // BIGSERIAL

    @Column(name = "source_topic", length = 128, nullable = false)
    private String sourceTopic;

    @Column(name = "tenant_id", length = 64)
    private String tenantId; // nullable

    @Column(name = "record_key", length = 256)
    private String recordKey; // nullable

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private JsonNode payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", columnDefinition = "jsonb")
    private JsonNode headers; // nullable

    @Column(name = "failed_at", nullable = false)
    private OffsetDateTime failedAt; // DB default now()

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
}
