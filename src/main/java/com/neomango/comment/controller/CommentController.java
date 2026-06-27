package com.neomango.comment.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.neomango.comment.dto.CommentCreateRequest;
import com.neomango.comment.dto.CommentResponse;
import com.neomango.comment.dto.CommentUpdateRequest;
import com.neomango.comment.service.CommentService;
import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.global.response.ApiResponse;
import com.neomango.global.security.AuthenticatedUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CommentController {

	private final CommentService commentService;

	@PostMapping("/api/posts/{postId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<CommentResponse> createComment(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long postId,
		@Valid @RequestBody CommentCreateRequest request
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(commentService.createComment(postId, currentUser.userId(), request));
	}

	@GetMapping("/api/posts/{postId}/comments")
	public ApiResponse<Page<CommentResponse>> getComments(
		@PathVariable Long postId,
		@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(commentService.getComments(postId, pageable));
	}

	@PatchMapping("/api/comments/{commentId}")
	public ApiResponse<CommentResponse> updateComment(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long commentId,
		@Valid @RequestBody CommentUpdateRequest request
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(commentService.updateComment(commentId, currentUser.userId(), request));
	}

	@DeleteMapping("/api/comments/{commentId}")
	public ApiResponse<Void> deleteComment(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long commentId
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		commentService.deleteComment(commentId, currentUser.userId());
		return ApiResponse.successWithoutData();
	}
}
