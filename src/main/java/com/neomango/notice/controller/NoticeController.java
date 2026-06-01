package com.neomango.notice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.neomango.global.response.ApiResponse;
import com.neomango.notice.dto.NoticeResponse;
import com.neomango.notice.dto.NoticeSummaryResponse;
import com.neomango.notice.service.NoticeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class NoticeController {

	private final NoticeService noticeService;

	@GetMapping("/api/notices")
	public ApiResponse<Page<NoticeSummaryResponse>> getNotices(@PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(noticeService.getNotices(pageable));
	}

	@GetMapping("/api/notices/{noticeId}")
	public ApiResponse<NoticeResponse> getNotice(@PathVariable Long noticeId) {
		return ApiResponse.success(noticeService.getNotice(noticeId));
	}
}
