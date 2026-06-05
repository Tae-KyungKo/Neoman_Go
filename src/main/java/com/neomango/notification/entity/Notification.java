package com.neomango.notification.entity;

import java.time.LocalDateTime;

import com.neomango.global.entity.BaseTimeEntity;
import com.neomango.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "notifications",
	indexes = {
		@Index(name = "idx_notifications_receiver_created_at", columnList = "receiver_id, created_at"),
		@Index(name = "idx_notifications_receiver_read_at", columnList = "receiver_id, read_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "receiver_id", nullable = false)
	private User receiver;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private NotificationType type;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 500)
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private NotificationTargetType targetType;

	@Column(nullable = false)
	private Long targetId;

	private LocalDateTime readAt;

	private Notification(
		User receiver,
		NotificationType type,
		String title,
		String message,
		NotificationTargetType targetType,
		Long targetId
	) {
		this.receiver = receiver;
		this.type = type;
		this.title = title;
		this.message = message;
		this.targetType = targetType;
		this.targetId = targetId;
	}

	public static Notification create(
		User receiver,
		NotificationType type,
		String title,
		String message,
		NotificationTargetType targetType,
		Long targetId
	) {
		return new Notification(receiver, type, title, message, targetType, targetId);
	}

	public void markAsRead() {
		if (this.readAt == null) {
			this.readAt = LocalDateTime.now();
		}
	}

	public boolean isRead() {
		return this.readAt != null;
	}
}
