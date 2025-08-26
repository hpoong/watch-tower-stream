package com.hopoong.user.adapter.kafka;

import com.hopoong.avro.user.UserEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class UserEventAvroPublisher {

	private final KafkaTemplate<String, Object> avroKafkaTemplate;

	@Autowired
	public UserEventAvroPublisher(@Qualifier("avroKafkaTemplate") KafkaTemplate<String, Object> avroKafkaTemplate) {
		this.avroKafkaTemplate = avroKafkaTemplate;
	}

	public void publishUserEvent(
			String topic,
			UUID id,
			String tenantId,
			String userId,
			String sessionId,
			String eventType,
			String feature,
			OffsetDateTime occurredAt,
			OffsetDateTime createdAt
	) {
		Objects.requireNonNull(topic, "topic must not be null");
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(tenantId, "tenantId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(eventType, "eventType must not be null");
		Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");

		UserEvent event = UserEvent.newBuilder()
				.setId(id)
				.setTenantId(tenantId)
				.setUserId(userId)
				.setSessionId(sessionId)
				.setEventType(eventType)
				.setFeature(feature)
				.setOccurredAt(occurredAt.toInstant())
				.setCreatedAt(createdAt.toInstant())
				.build();

		String recordKey = buildRecordKey(tenantId, userId, sessionId, id);

		avroKafkaTemplate.send(topic, recordKey, event)
			.whenComplete((result, ex) -> {
				if (ex != null) {
					log.error("Failed to publish UserEvent to topic {} with key {}", topic, recordKey, ex);
				} else {
					log.debug("Published UserEvent to topic {} partition {} offset {}", topic,
							result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
				}
			});
	}

	private String buildRecordKey(String tenantId, String userId, String sessionId, UUID id) {
		if (tenantId != null && userId != null && sessionId != null) {
			return tenantId + ":" + userId + ":" + sessionId;
		}
		if (tenantId != null && userId != null) {
			return tenantId + ":" + userId;
		}
		return id.toString();
	}
}