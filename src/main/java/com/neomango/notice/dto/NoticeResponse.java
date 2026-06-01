package com.neomango.notice.dto;

import java.time.LocalDateTime;

import com.neomango.notice.entity.Notice;

public record NoticeResponse(
	Long id,
	String title,
	String content,
	String authorName,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	private static final String PUBLIC_AUTHOR_NAME = "관리자";

	public static NoticeResponse from(Notice notice) {
		return new NoticeResponse(
			notice.getId(),
			notice.getTitle(),
			notice.getContent(),
			PUBLIC_AUTHOR_NAME,
			notice.getCreatedAt(),
			notice.getUpdatedAt()
		);
	}
}
