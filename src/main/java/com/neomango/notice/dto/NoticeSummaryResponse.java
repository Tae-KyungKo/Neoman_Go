package com.neomango.notice.dto;

import java.time.LocalDateTime;

import com.neomango.notice.entity.Notice;

public record NoticeSummaryResponse(
	Long id,
	String title,
	String authorName,
	LocalDateTime createdAt
) {

	private static final String PUBLIC_AUTHOR_NAME = "관리자";

	public static NoticeSummaryResponse from(Notice notice) {
		return new NoticeSummaryResponse(
			notice.getId(),
			notice.getTitle(),
			PUBLIC_AUTHOR_NAME,
			notice.getCreatedAt()
		);
	}
}
