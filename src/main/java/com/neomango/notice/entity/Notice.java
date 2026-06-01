package com.neomango.notice.entity;

import java.time.LocalDateTime;

import com.neomango.global.entity.BaseTimeEntity;
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
@Table(name = "notices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 5000)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NoticeStatus status;

	private LocalDateTime deletedAt;

	private Notice(User author, String title, String content) {
		this.author = author;
		this.title = title;
		this.content = content;
		this.status = NoticeStatus.ACTIVE;
	}

	public static Notice create(User author, String title, String content) {
		return new Notice(author, title, content);
	}

	public void update(String title, String content) {
		this.title = title;
		this.content = content;
	}

	public void softDelete() {
		this.status = NoticeStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
	}
}
