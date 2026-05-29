package com.neomango.post.dto;

import java.time.LocalDateTime;

import com.neomango.post.entity.Post;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserStatus;

public record PostResponse(
	Long id,
	String category,
	String title,
	String content,
	String authorNickname,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	private static final String DELETED_USER_NICKNAME = "탈퇴한 사용자";

	public static PostResponse from(Post post) {
		return new PostResponse(
			post.getId(),
			post.getCategory(),
			post.getTitle(),
			post.getContent(),
			authorNickname(post.getAuthor()),
			post.getCreatedAt(),
			post.getUpdatedAt()
		);
	}

	private static String authorNickname(User author) {
		if (author.getStatus() == UserStatus.DELETED) {
			return DELETED_USER_NICKNAME;
		}
		return author.getNickname();
	}
}
