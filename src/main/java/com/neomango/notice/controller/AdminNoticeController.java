package com.neomango.notice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.global.response.ApiResponse;
import com.neomango.global.security.AuthenticatedUser;
import com.neomango.notice.dto.NoticeCreateRequest;
import com.neomango.notice.dto.NoticeResponse;
import com.neomango.notice.dto.NoticeUpdateRequest;
import com.neomango.notice.service.NoticeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AdminNoticeController {

	private final NoticeService noticeService;

	@PostMapping("/api/admin/notices")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<NoticeResponse> createNotice(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@Valid @RequestBody NoticeCreateRequest request
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(noticeService.createNotice(currentUser.userId(), request));
	}

	@PatchMapping("/api/admin/notices/{noticeId}")
	public ApiResponse<NoticeResponse> updateNotice(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long noticeId,
		@Valid @RequestBody NoticeUpdateRequest request
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(noticeService.updateNotice(currentUser.userId(), noticeId, request));
	}

	@DeleteMapping("/api/admin/notices/{noticeId}")
	public ApiResponse<Void> deleteNotice(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long noticeId
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		noticeService.deleteNotice(currentUser.userId(), noticeId);
		return ApiResponse.successWithoutData();
	}
}
