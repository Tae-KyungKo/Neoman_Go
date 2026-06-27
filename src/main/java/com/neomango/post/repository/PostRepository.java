package com.neomango.post.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.neomango.post.entity.Post;
import com.neomango.post.entity.PostStatus;

public interface PostRepository extends JpaRepository<Post, Long> {

	@EntityGraph(attributePaths = "author")
	Page<Post> findByCategoryAndStatus(String category, PostStatus status, Pageable pageable);

	@EntityGraph(attributePaths = "author")
	Optional<Post> findByIdAndStatus(Long id, PostStatus status);

	boolean existsByIdAndStatus(Long id, PostStatus status);
}
