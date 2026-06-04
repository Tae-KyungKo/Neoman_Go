package com.neomango.notification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.notification.dto.NotificationResponse;
import com.neomango.notification.dto.UnreadNotificationCountResponse;
import com.neomango.notification.entity.Notification;
import com.neomango.notification.entity.NotificationTargetType;
import com.neomango.notification.entity.NotificationType;
import com.neomango.notification.exception.NotificationNotFoundException;
import com.neomango.notification.repository.NotificationRepository;
import com.neomango.user.entity.User;
import com.neomango.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;

	@Transactional
	public void createTeamApplicationCreatedNotification(
		Long receiverId,
		Long actorId,
		String teamName,
		String applicantNickname,
		Long applicationId
	) {
		createNotification(
			receiverId,
			actorId,
			NotificationType.TEAM_APPLICATION_CREATED,
			"팀 가입 신청",
			applicantNickname + "님이 " + teamName + " 팀에 가입 신청했습니다.",
			NotificationTargetType.TEAM_APPLICATION,
			applicationId
		);
	}

	@Transactional
	public void createTeamApplicationApprovedNotification(
		Long receiverId,
		Long actorId,
		String teamName,
		Long applicationId
	) {
		createNotification(
			receiverId,
			actorId,
			NotificationType.TEAM_APPLICATION_APPROVED,
			"가입 신청 승인",
			teamName + " 팀 가입 신청이 승인되었습니다.",
			NotificationTargetType.TEAM_APPLICATION,
			applicationId
		);
	}

	@Transactional
	public void createTeamApplicationRejectedNotification(
		Long receiverId,
		Long actorId,
		String teamName,
		Long applicationId
	) {
		createNotification(
			receiverId,
			actorId,
			NotificationType.TEAM_APPLICATION_REJECTED,
			"가입 신청 거절",
			teamName + " 팀 가입 신청이 거절되었습니다.",
			NotificationTargetType.TEAM_APPLICATION,
			applicationId
		);
	}

	public Page<NotificationResponse> getMyNotifications(Long userId, Pageable pageable) {
		validateAuthenticated(userId);

		Pageable latestPageable = PageRequest.of(
			pageable.getPageNumber(),
			pageable.getPageSize(),
			Sort.by(Sort.Direction.DESC, "createdAt", "id")
		);

		return notificationRepository.findByReceiverId(userId, latestPageable)
			.map(NotificationResponse::from);
	}

	public UnreadNotificationCountResponse getUnreadCount(Long userId) {
		validateAuthenticated(userId);

		return new UnreadNotificationCountResponse(
			notificationRepository.countByReceiverIdAndReadAtIsNull(userId)
		);
	}

	@Transactional
	public void markAsRead(Long userId, Long notificationId) {
		validateAuthenticated(userId);

		Notification notification = notificationRepository.findByIdAndReceiverId(notificationId, userId)
			.orElseThrow(NotificationNotFoundException::new);
		notification.markAsRead();
	}

	@Transactional
	public void markAllAsRead(Long userId) {
		validateAuthenticated(userId);

		notificationRepository.markAllAsReadByReceiverId(userId);
	}

	private void validateAuthenticated(Long userId) {
		if (userId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
	}

	private void createNotification(
		Long receiverId,
		Long actorId,
		NotificationType type,
		String title,
		String message,
		NotificationTargetType targetType,
		Long targetId
	) {
		if (receiverId != null && receiverId.equals(actorId)) {
			return;
		}

		User receiver = userRepository.getReferenceById(receiverId);
		notificationRepository.save(Notification.create(receiver, type, title, message, targetType, targetId));
	}
}
