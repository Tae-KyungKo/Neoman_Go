package com.neomango.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.neomango.comment.entity.Comment;
import com.neomango.comment.entity.CommentStatus;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	@EntityGraph(attributePaths = "author")
	Page<Comment> findByPostIdAndStatus(Long postId, CommentStatus status, Pageable pageable);
}
