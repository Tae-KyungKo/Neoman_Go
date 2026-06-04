package com.neomango.comment.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neomango.comment.dto.CommentCreateRequest;
import com.neomango.comment.dto.CommentResponse;
import com.neomango.comment.dto.CommentUpdateRequest;
import com.neomango.comment.entity.Comment;
import com.neomango.comment.entity.CommentStatus;
import com.neomango.comment.exception.CommentNotFoundException;
import com.neomango.comment.repository.CommentRepository;
import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.notification.service.NotificationService;
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
public class CommentService {

	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;

	public CommentResponse createComment(Long postId, Long authorId, CommentCreateRequest request) {
		if (authorId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		Post post = postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
			.orElseThrow(PostNotFoundException::new);
		User author = userRepository.findById(authorId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		validateActiveUser(author);

		Comment comment = Comment.create(post, author, request.content());
		Comment savedComment = commentRepository.save(comment);
		notificationService.createPostCommentCreatedNotification(
			post.getAuthor().getId(),
			author.getId(),
			post.getTitle(),
			author.getNickname(),
			post.getId()
		);

		return CommentResponse.from(savedComment);
	}

	@Transactional(readOnly = true)
	public Page<CommentResponse> getComments(Long postId, Pageable pageable) {
		if (!postRepository.existsByIdAndStatus(postId, PostStatus.ACTIVE)) {
			throw new PostNotFoundException();
		}

		Pageable oldestPageable = PageRequest.of(
			pageable.getPageNumber(),
			pageable.getPageSize(),
			Sort.by(Sort.Direction.ASC, "createdAt", "id")
		);

		return commentRepository.findByPostIdAndStatus(postId, CommentStatus.ACTIVE, oldestPageable)
			.map(CommentResponse::from);
	}

	public CommentResponse updateComment(Long commentId, Long requesterId, CommentUpdateRequest request) {
		if (requesterId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		Comment comment = commentRepository.findById(commentId)
			.orElseThrow(CommentNotFoundException::new);
		comment.getPost().validateNotDeleted();
		comment.update(request.content(), requesterId);

		return CommentResponse.from(comment);
	}

	public void deleteComment(Long commentId, Long requesterId) {
		if (requesterId == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		Comment comment = commentRepository.findById(commentId)
			.orElseThrow(CommentNotFoundException::new);
		comment.getPost().validateNotDeleted();
		comment.softDelete(requesterId);
	}

	private void validateActiveUser(User user) {
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
	}
}
