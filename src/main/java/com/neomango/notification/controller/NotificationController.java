package com.neomango.notification.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.global.response.ApiResponse;
import com.neomango.global.security.AuthenticatedUser;
import com.neomango.notification.dto.NotificationResponse;
import com.neomango.notification.dto.UnreadNotificationCountResponse;
import com.neomango.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	public ApiResponse<Page<NotificationResponse>> getMyNotifications(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PageableDefault(size = 20) Pageable pageable
	) {
		validateAuthenticated(currentUser);

		return ApiResponse.success(notificationService.getMyNotifications(currentUser.userId(), pageable));
	}

	@GetMapping("/unread-count")
	public ApiResponse<UnreadNotificationCountResponse> getUnreadCount(
		@AuthenticationPrincipal AuthenticatedUser currentUser
	) {
		validateAuthenticated(currentUser);

		return ApiResponse.success(notificationService.getUnreadCount(currentUser.userId()));
	}

	@PatchMapping("/{notificationId}/read")
	public ApiResponse<Void> markAsRead(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long notificationId
	) {
		validateAuthenticated(currentUser);

		notificationService.markAsRead(currentUser.userId(), notificationId);
		return ApiResponse.successWithoutData();
	}

	@PatchMapping("/read-all")
	public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal AuthenticatedUser currentUser) {
		validateAuthenticated(currentUser);

		notificationService.markAllAsRead(currentUser.userId());
		return ApiResponse.successWithoutData();
	}

	private void validateAuthenticated(AuthenticatedUser currentUser) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
	}
}
