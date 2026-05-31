package com.neomango.comment.dto;

import java.time.LocalDateTime;

import com.neomango.comment.entity.Comment;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserStatus;

public record CommentResponse(
	Long id,
	String content,
	String authorNickname,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	private static final String DELETED_USER_NICKNAME = "탈퇴한 사용자";

	public static CommentResponse from(Comment comment) {
		return new CommentResponse(
			comment.getId(),
			comment.getContent(),
			authorNickname(comment.getAuthor()),
			comment.getCreatedAt(),
			comment.getUpdatedAt()
		);
	}

	private static String authorNickname(User author) {
		if (author.getStatus() == UserStatus.DELETED) {
			return DELETED_USER_NICKNAME;
		}
		return author.getNickname();
	}
}
