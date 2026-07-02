package com.neomango.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.neomango.post.policy.PostPolicy;

public record PostCreateRequest(
	@NotBlank(message = "게시글 제목은 필수입니다.")
	@Size(
		min = PostPolicy.TITLE_MIN_LENGTH,
		max = PostPolicy.TITLE_MAX_LENGTH,
		message = "게시글 제목은 1자 이상 100자 이하여야 합니다."
	)
	String title,

	@NotBlank(message = "게시글 내용은 필수입니다.")
	@Size(
		min = PostPolicy.CONTENT_MIN_LENGTH,
		max = PostPolicy.CONTENT_MAX_LENGTH,
		message = "게시글 내용은 1자 이상 5000자 이하여야 합니다."
	)
	String content
) {
}
