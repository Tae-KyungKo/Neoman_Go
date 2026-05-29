package com.neomango.post.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.post.dto.PostCreateRequest;
import com.neomango.post.dto.PostResponse;
import com.neomango.post.dto.PostSummaryResponse;
import com.neomango.post.dto.PostUpdateRequest;
import com.neomango.post.entity.Post;
import com.neomango.post.entity.PostStatus;
import com.neomango.post.exception.PostNotFoundException;
import com.neomango.post.repository.PostRepository;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserStatus;
import com.neomango.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

	private final PostRepository postRepository;
	private final UserRepository userRepository;

	public PostResponse createPost(Long authorId, String category, PostCreateRequest request) {
		if (authorId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		User author = userRepository.findById(authorId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		validateActiveUser(author);

		Post post = Post.create(normalizeCategory(category), request.title(), request.content(), author);
		return PostResponse.from(postRepository.save(post));
	}

	@Transactional(readOnly = true)
	public Page<PostSummaryResponse> getPosts(String category, Pageable pageable) {
		Pageable latestPageable = PageRequest.of(
			pageable.getPageNumber(),
			pageable.getPageSize(),
			Sort.by(Sort.Direction.DESC, "createdAt", "id")
		);

		return postRepository.findByCategoryAndStatus(normalizeCategory(category), PostStatus.ACTIVE, latestPageable)
			.map(PostSummaryResponse::from);
	}

	@Transactional(readOnly = true)
	public PostResponse getPost(Long postId) {
		Post post = postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
			.orElseThrow(PostNotFoundException::new);

		return PostResponse.from(post);
	}

	public PostResponse updatePost(Long postId, Long requesterId, PostUpdateRequest request) {
		if (requesterId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		Post post = postRepository.findById(postId)
			.orElseThrow(PostNotFoundException::new);
		post.update(request.title(), request.content(), requesterId);

		return PostResponse.from(post);
	}

	public void deletePost(Long postId, Long requesterId) {
		if (requesterId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		Post post = postRepository.findById(postId)
			.orElseThrow(PostNotFoundException::new);
		post.softDelete(requesterId);
	}

	private void validateActiveUser(User user) {
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
	}

	private String normalizeCategory(String category) {
		if (!StringUtils.hasText(category)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		// TODO: Team/Post category 문자열 표준화를 위해 enum 또는 Category 테이블 분리를 검토한다.
		return category.trim();
	}
}
