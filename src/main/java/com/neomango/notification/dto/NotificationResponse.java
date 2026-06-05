package com.neomango.notification.dto;

import java.time.LocalDateTime;

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
	LocalDateTime createdAt
) {

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
			notification.getCreatedAt()
		);
	}
}
