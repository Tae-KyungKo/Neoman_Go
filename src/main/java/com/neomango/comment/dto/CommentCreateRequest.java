package com.neomango.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.neomango.comment.policy.CommentPolicy;

public record CommentCreateRequest(
	@NotBlank(message = "댓글 내용은 필수입니다.")
	@Size(
		min = CommentPolicy.CONTENT_MIN_LENGTH,
		max = CommentPolicy.CONTENT_MAX_LENGTH,
		message = "댓글 내용은 1자 이상 1000자 이하여야 합니다."
	)
	String content
) {
}
