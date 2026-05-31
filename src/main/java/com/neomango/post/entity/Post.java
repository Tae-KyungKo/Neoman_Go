package com.neomango.post.entity;

import java.time.LocalDateTime;

import com.neomango.post.exception.PostAccessDeniedException;
import com.neomango.post.exception.PostNotFoundException;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String category;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 5000)
	private String content;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PostStatus status;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	private LocalDateTime deletedAt;

	private Post(String category, String title, String content, User author) {
		this.category = category;
		this.title = title;
		this.content = content;
		this.author = author;
		this.status = PostStatus.ACTIVE;
	}

	public static Post create(String category, String title, String content, User author) {
		return new Post(category, title, content, author);
	}

	public void update(String title, String content, Long requesterId) {
		validateNotDeleted();
		validateAuthor(requesterId);
		this.title = title;
		this.content = content;
	}

	public void softDelete(Long requesterId) {
		validateNotDeleted();
		validateAuthor(requesterId);
		this.status = PostStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
	}

	public boolean isDeleted() {
		return this.status == PostStatus.DELETED;
	}

	public void validateAuthor(Long requesterId) {
		if (!this.author.getId().equals(requesterId)) {
			throw new PostAccessDeniedException();
		}
	}

	public void validateNotDeleted() {
		if (isDeleted()) {
			throw new PostNotFoundException();
		}
	}

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
