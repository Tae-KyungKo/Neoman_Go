package com.neomango.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neomango.auth.jwt.JwtTokenProvider;
import com.neomango.post.dto.PostCreateRequest;
import com.neomango.post.dto.PostUpdateRequest;
import com.neomango.post.entity.Post;
import com.neomango.post.entity.PostStatus;
import com.neomango.post.repository.PostRepository;
import com.neomango.team.repository.TeamApplicationRepository;
import com.neomango.team.repository.TeamMemberRepository;
import com.neomango.team.repository.TeamRepository;
import com.neomango.team.repository.UserCategoryMembershipRepository;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;
import com.neomango.user.repository.UserRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class PostControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private TeamApplicationRepository teamApplicationRepository;

	@Autowired
	private UserCategoryMembershipRepository userCategoryMembershipRepository;

	@Autowired
	private TeamMemberRepository teamMemberRepository;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		teamApplicationRepository.deleteAll();
		userCategoryMembershipRepository.deleteAll();
		teamMemberRepository.deleteAll();
		teamRepository.deleteAll();
		postRepository.deleteAll();
		userRepository.deleteAll();
	}

	@AfterEach
	void tearDown() {
		teamApplicationRepository.deleteAll();
		userCategoryMembershipRepository.deleteAll();
		teamMemberRepository.deleteAll();
		teamRepository.deleteAll();
		postRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void createPostReturnsCreatedWhenAuthenticated() throws Exception {
		User user = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		String accessToken = accessToken(user);
		PostCreateRequest request = new PostCreateRequest("title", "content");

		mockMvc.perform(post("/api/categories/{category}/posts", "GAME")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.category").value("GAME"))
			.andExpect(jsonPath("$.data.title").value("title"))
			.andExpect(jsonPath("$.data.content").value("content"))
			.andExpect(jsonPath("$.data.authorNickname").value("author"));

		Post post = postRepository.findAll().get(0);
		assertThat(post.getAuthor().getId()).isEqualTo(user.getId());
	}

	@Test
	void createPostRejectsRequestWithoutAuthentication() throws Exception {
		PostCreateRequest request = new PostCreateRequest("title", "content");

		mockMvc.perform(post("/api/categories/{category}/posts", "GAME")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void createPostRejectsBlankTitle() throws Exception {
		User user = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		PostCreateRequest request = new PostCreateRequest(" ", "content");

		mockMvc.perform(post("/api/categories/{category}/posts", "GAME")
				.header("Authorization", "Bearer " + accessToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));
	}

	@Test
	void createPostRejectsBlankContent() throws Exception {
		User user = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		PostCreateRequest request = new PostCreateRequest("title", " ");

		mockMvc.perform(post("/api/categories/{category}/posts", "GAME")
				.header("Authorization", "Bearer " + accessToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));
	}

	@Test
	void createPostRejectsTooLongTitle() throws Exception {
		User user = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		PostCreateRequest request = new PostCreateRequest("a".repeat(101), "content");

		mockMvc.perform(post("/api/categories/{category}/posts", "GAME")
				.header("Authorization", "Bearer " + accessToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));
	}

	@Test
	void createPostRejectsTooLongContent() throws Exception {
		User user = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		PostCreateRequest request = new PostCreateRequest("title", "a".repeat(5001));

		mockMvc.perform(post("/api/categories/{category}/posts", "GAME")
				.header("Authorization", "Bearer " + accessToken(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));
	}

	@Test
	void getPostsReturnsOnlyActivePostsInCategoryWithPagingAndLatestOrder() throws Exception {
		User author = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		Post oldPost = postRepository.save(Post.create("GAME", "old", "old content", author));
		Post latestPost = postRepository.save(Post.create("GAME", "latest", "latest content", author));
		Post otherCategoryPost = postRepository.save(Post.create("SPORTS", "sports", "sports content", author));
		Post deletedPost = postRepository.save(Post.create("GAME", "deleted", "deleted content", author));
		deletedPost.softDelete(author.getId());
		postRepository.saveAndFlush(deletedPost);

		mockMvc.perform(get("/api/categories/{category}/posts", "GAME")
				.param("page", "0")
				.param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.content[0].id").value(latestPost.getId()))
			.andExpect(jsonPath("$.data.content[0].title").value("latest"))
			.andExpect(jsonPath("$.data.content[0].category").value("GAME"))
			.andExpect(jsonPath("$.data.content[0].authorNickname").value("author"))
			.andExpect(jsonPath("$.data.content[0].content").doesNotExist())
			.andExpect(jsonPath("$.data.content[0].authorId").doesNotExist())
			.andExpect(jsonPath("$.data.totalElements").value(2));

		assertThat(oldPost.getId()).isNotEqualTo(latestPost.getId());
		assertThat(otherCategoryPost.getCategory()).isEqualTo("SPORTS");
	}

	@Test
	void getPostsShowsDeletedUserNickname() throws Exception {
		User author = userRepository.save(User.create("deleted-author@test.com", "encoded-password", "author"));
		postRepository.save(Post.create("GAME", "title", "content", author));
		author.softDelete();
		userRepository.saveAndFlush(author);

		mockMvc.perform(get("/api/categories/{category}/posts", "GAME"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].authorNickname").value("탈퇴한 사용자"));
	}

	@Test
	void getPostReturnsActivePostDetailWithoutAuthentication() throws Exception {
		User author = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));

		mockMvc.perform(get("/api/posts/{postId}", post.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(post.getId()))
			.andExpect(jsonPath("$.data.title").value("title"))
			.andExpect(jsonPath("$.data.content").value("content"))
			.andExpect(jsonPath("$.data.category").value("GAME"))
			.andExpect(jsonPath("$.data.authorNickname").value("author"))
			.andExpect(jsonPath("$.data.authorId").doesNotExist())
			.andExpect(jsonPath("$.data.email").doesNotExist())
			.andExpect(jsonPath("$.data.role").doesNotExist())
			.andExpect(jsonPath("$.data.status").doesNotExist())
			.andExpect(jsonPath("$.data.createdAt").exists())
			.andExpect(jsonPath("$.data.updatedAt").exists());
	}

	@Test
	void getPostRejectsDeletedPost() throws Exception {
		User author = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		post.softDelete(author.getId());
		postRepository.saveAndFlush(post);

		mockMvc.perform(get("/api/posts/{postId}", post.getId()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("P001"));
	}

	@Test
	void getPostShowsDeletedUserNickname() throws Exception {
		User author = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		author.softDelete();
		userRepository.saveAndFlush(author);

		mockMvc.perform(get("/api/posts/{postId}", post.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.authorNickname").value("탈퇴한 사용자"));
	}

	@Test
	void updatePostSucceedsWhenRequesterIsAuthor() throws Exception {
		User author = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		PostUpdateRequest request = new PostUpdateRequest("updated", "updated content");

		mockMvc.perform(patch("/api/posts/{postId}", post.getId())
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.title").value("updated"))
			.andExpect(jsonPath("$.data.content").value("updated content"));

		Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
		assertThat(updatedPost.getTitle()).isEqualTo("updated");
		assertThat(updatedPost.getContent()).isEqualTo("updated content");
	}

	@Test
	void updatePostRejectsRequesterWhoIsNotAuthor() throws Exception {
		User author = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		User other = userRepository.save(User.create("other@test.com", "encoded-password", "other"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		PostUpdateRequest request = new PostUpdateRequest("updated", "updated content");

		mockMvc.perform(patch("/api/posts/{postId}", post.getId())
				.header("Authorization", "Bearer " + accessToken(other))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("P002"));
	}

	@Test
	void updatePostRejectsRequestWithoutAuthentication() throws Exception {
		User author = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		PostUpdateRequest request = new PostUpdateRequest("updated", "updated content");

		mockMvc.perform(patch("/api/posts/{postId}", post.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void updatePostRejectsBlankContent() throws Exception {
		User author = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		PostUpdateRequest request = new PostUpdateRequest("updated", " ");

		mockMvc.perform(patch("/api/posts/{postId}", post.getId())
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));
	}

	@Test
	void updatePostRejectsDeletedPost() throws Exception {
		User author = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		post.softDelete(author.getId());
		postRepository.saveAndFlush(post);
		PostUpdateRequest request = new PostUpdateRequest("updated", "updated content");

		mockMvc.perform(patch("/api/posts/{postId}", post.getId())
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("P001"));
	}

	@Test
	void deletePostSoftDeletesWhenRequesterIsAuthor() throws Exception {
		User author = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));

		mockMvc.perform(delete("/api/posts/{postId}", post.getId())
				.header("Authorization", "Bearer " + accessToken(author)))
			.andExpect(status().isOk());

		Post deletedPost = postRepository.findById(post.getId()).orElseThrow();
		assertThat(deletedPost.getStatus()).isEqualTo(PostStatus.DELETED);
		assertThat(deletedPost.getDeletedAt()).isNotNull();

		mockMvc.perform(get("/api/categories/{category}/posts", "GAME"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(0));
		mockMvc.perform(get("/api/posts/{postId}", post.getId()))
			.andExpect(status().isNotFound());
	}

	@Test
	void deletePostRejectsRequesterWhoIsNotAuthor() throws Exception {
		User author = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		User other = userRepository.save(User.create("other@test.com", "encoded-password", "other"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));

		mockMvc.perform(delete("/api/posts/{postId}", post.getId())
				.header("Authorization", "Bearer " + accessToken(other)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("P002"));
	}

	@Test
	void deletePostRejectsRequestWithoutAuthentication() throws Exception {
		User author = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));

		mockMvc.perform(delete("/api/posts/{postId}", post.getId()))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void deletePostRejectsDeletedPost() throws Exception {
		User author = userRepository.save(User.create("author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		post.softDelete(author.getId());
		postRepository.saveAndFlush(post);

		mockMvc.perform(delete("/api/posts/{postId}", post.getId())
				.header("Authorization", "Bearer " + accessToken(author)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("P001"));
	}

	private String accessToken(User user) {
		return jwtTokenProvider.createAccessToken(user.getId(), UserRole.USER);
	}
}
