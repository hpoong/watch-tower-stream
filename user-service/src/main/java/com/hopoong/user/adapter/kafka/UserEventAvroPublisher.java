package com.hopoong.user.adapter.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class UserEventAvroPublisher {

	private static final Schema USER_EVENT_SCHEMA = loadSchema();

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

		GenericRecord record = new GenericRecordBuilder(USER_EVENT_SCHEMA)
				.set("id", id.toString())
				.set("tenant_id", tenantId)
				.set("user_id", userId)
				.set("session_id", sessionId)
				.set("event_type", eventType)
				.set("feature", feature)
				.set("occurred_at", occurredAt.toInstant().toEpochMilli())
				.set("created_at", createdAt.toInstant().toEpochMilli())
				.build();

		String recordKey = buildRecordKey(tenantId, userId, sessionId, id);

		avroKafkaTemplate.send(topic, recordKey, record)
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

	private static final String SCHEMA_PATH = "/avro/UserEvent.avsc";

	private static Schema loadSchema() {
		try (InputStream is = UserEventAvroPublisher.class.getResourceAsStream(SCHEMA_PATH)) {
			if (is == null) {
				throw new IllegalStateException("Avro schema not found: " + SCHEMA_PATH);
			}
			return new Schema.Parser().parse(is);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load/parse Avro schema: " + SCHEMA_PATH, e);
		}
	}
}