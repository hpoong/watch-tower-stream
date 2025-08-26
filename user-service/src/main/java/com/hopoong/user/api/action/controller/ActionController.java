package com.hopoong.user.api.action.controller;

import com.hopoong.avro.record.user.UserActionEventRecord;
import com.hopoong.user.event.UserActionEventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/user/action")
@RequiredArgsConstructor
public class ActionController {

	private final UserActionEventHandler userActionEventHandler;

	@GetMapping("/events/mock")
	public ResponseEntity<Void> publishMockUserEvent() {
		UUID id = UUID.randomUUID();

		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		String tenantId = "t-" + rnd.nextInt(1000, 9999);
		String userId = "u-" + rnd.nextInt(1000, 9999);
		String sessionId = UUID.randomUUID().toString();

		String[] eventTypes = {"CLICK", "VIEW", "LOGIN", "LOGOUT", "NAVIGATE"};
		String[] features = {"DASHBOARD", "SETTINGS", "REPORTS", "PROFILE", null};

		String eventType = eventTypes[rnd.nextInt(eventTypes.length)];
		String feature = features[rnd.nextInt(features.length)];

		OffsetDateTime createdAt = OffsetDateTime.now();
		OffsetDateTime occurredAt = createdAt.minusSeconds(rnd.nextInt(0, 60));

		UserActionEventRecord body = UserActionEventRecord.newBuilder()
				.setId(id)
				.setTenantId(tenantId)
				.setUserId(userId)
				.setSessionId(sessionId)
				.setEventType(eventType)
				.setFeature(feature)
				.setOccurredAt(occurredAt.toInstant())
				.setCreatedAt(createdAt.toInstant())
				.build();

		userActionEventHandler.handleSystemResourceMetricsEvent(body);

		return ResponseEntity.accepted().build();
	}
}