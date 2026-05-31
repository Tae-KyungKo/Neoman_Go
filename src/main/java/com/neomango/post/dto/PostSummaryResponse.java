package com.neomango.post.dto;

import java.time.LocalDateTime;

import com.neomango.post.entity.Post;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserStatus;

public record PostSummaryResponse(
	Long id,
	String category,
	String title,
	String authorNickname,
	LocalDateTime createdAt
) {

	private static final String DELETED_USER_NICKNAME = "탈퇴한 사용자";

	public static PostSummaryResponse from(Post post) {
		return new PostSummaryResponse(
			post.getId(),
			post.getCategory(),
			post.getTitle(),
			authorNickname(post.getAuthor()),
			post.getCreatedAt()
		);
	}

	private static String authorNickname(User author) {
		if (author.getStatus() == UserStatus.DELETED) {
			return DELETED_USER_NICKNAME;
		}
		return author.getNickname();
	}
}
