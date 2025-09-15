package com.hopoong.audit.usecase.user.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopoong.audit.persistence.entity.OutboxEventEntity;
import com.hopoong.audit.persistence.entity.UserActionEventEntity;
import com.hopoong.audit.repository.DltStoreJpaRepository;
import com.hopoong.audit.repository.OutboxEventJpaRepository;
import com.hopoong.audit.repository.UserActionEventJpaRepository;
import com.hopoong.avro.common.CommonHeaderRecord;
import com.hopoong.avro.message.UserActionEventMessage;
import com.hopoong.avro.record.user.UserActionEventRecord;
import com.hopoong.core.topic.KafkaTopicManager;
import com.hopoong.core.util.AvroJsonUtil;
import com.hopoong.core.util.LoggerUtil;
import com.sun.jdi.request.DuplicateRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionService {

    private final UserActionEventJpaRepository userActionEventJpaRepository;
    private final OutboxEventJpaRepository outboxEventJpaRepository;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // 멱등 TTL: 요청 재전송 허용 윈도우
    private static final long IDEM_TTL_SEC = 600;


    @Transactional
    public UUID registerUserAction(ConsumerRecord<String, UserActionEventMessage> data) throws Exception {
        LoggerUtil.section(log, "user-action-events :: DB 저장");

        CommonHeaderRecord header = data.value().getHeader();
        UserActionEventRecord body = data.value().getBody();

        final String idempotencyKey = buildIdemKey(body.getTenantId(), header.getTraceId());
        final boolean firstSeen = setIdempotencyKey(idempotencyKey);

        // 중복 체크
        if (!firstSeen) {
            throw new DuplicateRequestException("duplicate request: " + header.getTraceId());
        }

        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // 1) user_action_event INSERT
        UserActionEventEntity event = userActionEventJpaRepository.save(
                UserActionEventEntity.builder()
                        .tenantId(body.getTenantId())
                        .userId(body.getUserId())
                        .sessionId(body.getSessionId())
                        .eventType(body.getEventType())
                        .feature(body.getFeature())
                        .occurredAt(now)
                        .createdAt(now)
                        .build()
        );

        // 2) outbox_event INSERT
        final String recordKey = body.getTenantId() + ":" + body.getUserId();
        UUID eventId = event.getId();

        Map<String, Object> headers = Map.of(
                "traceId", header.getTraceId(),
                "schema", "UserActionEventMessage",
                "encoding", "avro"
        );

        OutboxEventEntity outbox = OutboxEventEntity.builder()
                .aggregateType(KafkaTopicManager.USER_ACTION_EVENTS) // user-action-events
                .aggregateId(eventId)
                .tenantId(body.getTenantId())
                .topic(KafkaTopicManager.USER_ACTION_EVENTS_ENRICHED_V1) // user-action-events-enriched.v1
                .recordKey(recordKey)
                .payload(AvroJsonUtil.toJsonNode(data.value()))
                .headers(objectMapper.valueToTree(headers))
                .createdAt(now)
                .publishedAt(null)
                .errorCount(0)
                .build();

        outboxEventJpaRepository.save(outbox);

        // 커밋 후 멱등키 eventId를 덮어써서 eventId 반환
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(int status) { // 실패
                if (status == STATUS_ROLLED_BACK) {
                    redisTemplate.delete(idempotencyKey);
                }
            }
            @Override
            public void afterCommit() { // 성공
                redisTemplate.opsForValue().set(idempotencyKey, eventId, IDEM_TTL_SEC, TimeUnit.SECONDS);
            }
        });

        return eventId;
    }

    private String buildIdemKey(String tenantId, String traceId) {
        return "user-activity" + ":" + tenantId + ":" + traceId;
    }

    private boolean setIdempotencyKey(String key) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1", IDEM_TTL_SEC, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);
    }

}
