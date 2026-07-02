package com.neomango.notification.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.neomango.notification.entity.Notification;
import com.neomango.notification.entity.NotificationTargetType;
import com.neomango.notification.entity.NotificationType;

public record NotificationResponse(
	Long id,
	NotificationType type,
	String title,
	String message,
	NotificationTargetType targetType,
	Long targetId,
	boolean read,
	LocalDateTime readAt,
	OffsetDateTime createdAt
) {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public static NotificationResponse from(Notification notification) {
		return new NotificationResponse(
			notification.getId(),
			notification.getType(),
			notification.getTitle(),
			notification.getMessage(),
			notification.getTargetType(),
			notification.getTargetId(),
			notification.isRead(),
			notification.getReadAt(),
			toKst(notification.getCreatedAt())
		);
	}

	private static OffsetDateTime toKst(LocalDateTime createdAt) {
		if (createdAt == null) {
			return null;
		}

		return createdAt
			.atZone(ZoneId.systemDefault())
			.withZoneSameInstant(KST)
			.toOffsetDateTime();
	}
}
