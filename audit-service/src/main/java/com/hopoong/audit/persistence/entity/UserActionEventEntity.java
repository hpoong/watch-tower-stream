package com.hopoong.audit.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/* =========================
   user_event (파티션 부모)
   PK: id
   PARTITION BY RANGE (occurred_at)
   ========================= */
@Entity
@Table(name = "user_action_event", schema = "um")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserActionEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "user_id", length = 128, nullable = false)
    private String userId;

    @Column(name = "session_id", length = 128)
    private String sessionId; // 앱 시작, 브라우저 탭 오픈 시 발급

    @Column(name = "event_type", length = 64, nullable = false) // CLICK/VIEW/ENTER/LEAVE/ERROR...
    private String eventType;

    @Column(name = "feature", length = 128)
    private String feature; // 기능 단위

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt; // 이벤트 실제 발생 시각(UTC)

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt; // DB default now(); 앱에서 굳이 세팅 안 해도 됨
}