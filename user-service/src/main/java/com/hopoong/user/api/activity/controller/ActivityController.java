package com.hopoong.user.api.activity.controller;

import com.hopoong.user.adapter.kafka.UserEventAvroPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/activity")
@RequiredArgsConstructor
public class ActivityController {

	private final UserEventAvroPublisher userEventAvroPublisher;

	@PostMapping("/events/mock")
	public ResponseEntity<Void> publishMockUserEvent() {
		String topic = "user.activity.events";
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

		userEventAvroPublisher.publishUserEvent(
				topic,
				id,
				tenantId,
				userId,
				sessionId,
				eventType,
				feature,
				occurredAt,
				createdAt
		);

		return ResponseEntity.accepted().build();
	}
}