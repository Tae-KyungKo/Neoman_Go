package com.neomango.post.controller;

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

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.global.response.ApiResponse;
import com.neomango.global.security.AuthenticatedUser;
import com.neomango.post.dto.PostCreateRequest;
import com.neomango.post.dto.PostResponse;
import com.neomango.post.dto.PostSummaryResponse;
import com.neomango.post.dto.PostUpdateRequest;
import com.neomango.post.service.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@PostMapping("/api/categories/{category}/posts")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<PostResponse> createPost(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable String category,
		@Valid @RequestBody PostCreateRequest request
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(postService.createPost(currentUser.userId(), category, request));
	}

	@GetMapping("/api/categories/{category}/posts")
	public ApiResponse<Page<PostSummaryResponse>> getPosts(
		@PathVariable String category,
		@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(postService.getPosts(category, pageable));
	}

	@GetMapping("/api/posts/{postId}")
	public ApiResponse<PostResponse> getPost(@PathVariable Long postId) {
		return ApiResponse.success(postService.getPost(postId));
	}

	@PatchMapping("/api/posts/{postId}")
	public ApiResponse<PostResponse> updatePost(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long postId,
		@Valid @RequestBody PostUpdateRequest request
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		return ApiResponse.success(postService.updatePost(postId, currentUser.userId(), request));
	}

	@DeleteMapping("/api/posts/{postId}")
	public ApiResponse<Void> deletePost(
		@AuthenticationPrincipal AuthenticatedUser currentUser,
		@PathVariable Long postId
	) {
		if (currentUser == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		postService.deletePost(postId, currentUser.userId());
		return ApiResponse.successWithoutData();
	}
}
