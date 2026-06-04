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
import com.neomango.notification.exception.NotificationNotFoundException;
import com.neomango.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

	private final NotificationRepository notificationRepository;

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
}
