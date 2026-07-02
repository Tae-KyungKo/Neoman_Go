package com.neomango.notification.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.neomango.notification.entity.Notification;
import com.neomango.notification.entity.NotificationTargetType;
import com.neomango.notification.entity.NotificationType;
import com.neomango.user.entity.User;

class NotificationResponseTest {

	@Test
	void fromConvertsCreatedAtToKstOffsetDateTime() {
		User receiver = User.create(
			com.neomango.support.TestLoginIds.next(),
			"notification-response@test.com",
			"encoded-password",
			"notificationResponse"
		);
		Notification notification = Notification.create(
			receiver,
			NotificationType.TEAM_APPLICATION_CREATED,
			"title",
			"message",
			NotificationTargetType.TEAM_APPLICATION,
			1L
		);
		ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.of(2026, 7, 1, 22, 30));

		NotificationResponse response = NotificationResponse.from(notification);

		assertThat(response.createdAt()).isNotNull();
		assertThat(response.createdAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
		assertThat(response.createdAt().toString()).endsWith("+09:00");
	}
}
