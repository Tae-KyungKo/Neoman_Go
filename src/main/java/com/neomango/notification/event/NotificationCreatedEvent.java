package com.neomango.notification.event;

public record NotificationCreatedEvent(
	Long notificationId,
	Long receiverId
) {
}
