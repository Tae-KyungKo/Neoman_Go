package com.neomango.comment.entity;

import java.time.LocalDateTime;

import com.neomango.comment.exception.CommentAccessDeniedException;
import com.neomango.comment.exception.CommentNotFoundException;
import com.neomango.global.entity.BaseTimeEntity;
import com.neomango.post.entity.Post;
import com.neomango.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(nullable = false, length = 1000)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CommentStatus status;

	private LocalDateTime deletedAt;

	private Comment(Post post, User author, String content) {
		this.post = post;
		this.author = author;
		this.content = content;
		this.status = CommentStatus.ACTIVE;
	}

	public static Comment create(Post post, User author, String content) {
		return new Comment(post, author, content);
	}

	public void update(String content, Long requesterId) {
		validateNotDeleted();
		validateAuthor(requesterId);
		this.content = content;
	}

	public void softDelete(Long requesterId) {
		validateNotDeleted();
		validateAuthor(requesterId);
		this.status = CommentStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isDeleted() {
		return this.status == CommentStatus.DELETED;
	}

	public void validateAuthor(Long requesterId) {
		if (!this.author.getId().equals(requesterId)) {
			throw new CommentAccessDeniedException();
		}
	}

	public void validateNotDeleted() {
		if (isDeleted()) {
			throw new CommentNotFoundException();
		}
	}
}
