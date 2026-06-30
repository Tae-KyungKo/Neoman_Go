package com.neomango.comment.controller;

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
import com.neomango.comment.dto.CommentCreateRequest;
import com.neomango.comment.dto.CommentUpdateRequest;
import com.neomango.comment.entity.Comment;
import com.neomango.comment.entity.CommentStatus;
import com.neomango.comment.repository.CommentRepository;
import com.neomango.notification.entity.Notification;
import com.neomango.notification.entity.NotificationTargetType;
import com.neomango.notification.entity.NotificationType;
import com.neomango.notification.repository.NotificationRepository;
import com.neomango.post.entity.Post;
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
class CommentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private NotificationRepository notificationRepository;

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
		deleteAll();
	}

	@AfterEach
	void tearDown() {
		deleteAll();
	}

	@Test
	void createCommentReturnsCreatedWhenAuthenticated() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		CommentCreateRequest request = new CommentCreateRequest("comment");

		mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.content").value("comment"))
			.andExpect(jsonPath("$.data.authorNickname").value("author"))
			.andExpect(jsonPath("$.data.authorId").doesNotExist());

		Comment comment = commentRepository.findAll().get(0);
		assertThat(comment.getPost().getId()).isEqualTo(post.getId());
		assertThat(comment.getAuthor().getId()).isEqualTo(author.getId());
	}

	@Test
	void createCommentCreatesPostCommentNotificationToPostAuthor() throws Exception {
		User postAuthor = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "post-author@test.com", "encoded-password", "postAuthor"));
		User commentAuthor = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "comment-author@test.com", "encoded-password", "commentAuthor"));
		Post post = postRepository.save(Post.create("GAME", "post title", "content", postAuthor));
		CommentCreateRequest request = new CommentCreateRequest("comment");

		mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
				.header("Authorization", "Bearer " + accessToken(commentAuthor))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated());

		assertThat(notificationRepository.count()).isEqualTo(1);
		Notification notification = notificationRepository.findAll().get(0);
		assertThat(notification.getReceiver().getId()).isEqualTo(postAuthor.getId());
		assertThat(notification.getType()).isEqualTo(NotificationType.POST_COMMENT_CREATED);
		assertThat(notification.getTitle()).isEqualTo("새 댓글");
		assertThat(notification.getMessage()).contains("commentAuthor", "post title");
		assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.POST);
		assertThat(notification.getTargetId()).isEqualTo(post.getId());
	}

	@Test
	void createCommentDoesNotCreateNotificationWhenAuthorCommentsOwnPost() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		CommentCreateRequest request = new CommentCreateRequest("comment");

		mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated());

		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void createCommentRejectsRequestWithoutAuthentication() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		CommentCreateRequest request = new CommentCreateRequest("comment");

		mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized());

		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void createCommentRejectsMissingPost() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		CommentCreateRequest request = new CommentCreateRequest("comment");

		mockMvc.perform(post("/api/posts/{postId}/comments", 999L)
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("P001"));
	}

	@Test
	void createCommentRejectsDeletedPost() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		post.softDelete(author.getId());
		postRepository.saveAndFlush(post);
		CommentCreateRequest request = new CommentCreateRequest("comment");

		mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("P001"));

		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void getCommentsReturnsOnlyActiveCommentsInOldestOrder() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		Comment oldComment = commentRepository.save(Comment.create(post, author, "old"));
		Comment latestComment = commentRepository.save(Comment.create(post, author, "latest"));
		Comment deletedComment = commentRepository.save(Comment.create(post, author, "deleted"));
		deletedComment.softDelete(author.getId());
		commentRepository.saveAndFlush(deletedComment);

		mockMvc.perform(get("/api/posts/{postId}/comments", post.getId())
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(2))
			.andExpect(jsonPath("$.data.content[0].id").value(oldComment.getId()))
			.andExpect(jsonPath("$.data.content[0].content").value("old"))
			.andExpect(jsonPath("$.data.content[1].id").value(latestComment.getId()))
			.andExpect(jsonPath("$.data.totalElements").value(2));
	}

	@Test
	void getCommentsRejectsDeletedPost() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		post.softDelete(author.getId());
		postRepository.saveAndFlush(post);

		mockMvc.perform(get("/api/posts/{postId}/comments", post.getId()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("P001"));
	}

	@Test
	void updateCommentSucceedsWhenRequesterIsAuthor() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		Comment comment = commentRepository.save(Comment.create(post, author, "comment"));
		CommentUpdateRequest request = new CommentUpdateRequest("updated");

		mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content").value("updated"));

		Comment updatedComment = commentRepository.findById(comment.getId()).orElseThrow();
		assertThat(updatedComment.getContent()).isEqualTo("updated");
	}

	@Test
	void updateCommentRejectsRequesterWhoIsNotAuthor() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		User other = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "other@test.com", "encoded-password", "other"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		Comment comment = commentRepository.save(Comment.create(post, author, "comment"));
		CommentUpdateRequest request = new CommentUpdateRequest("updated");

		mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
				.header("Authorization", "Bearer " + accessToken(other))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("C002"));
	}

	@Test
	void deleteCommentSoftDeletesWhenRequesterIsAuthor() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		Comment comment = commentRepository.save(Comment.create(post, author, "comment"));

		mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
				.header("Authorization", "Bearer " + accessToken(author)))
			.andExpect(status().isOk());

		Comment deletedComment = commentRepository.findById(comment.getId()).orElseThrow();
		assertThat(deletedComment.getStatus()).isEqualTo(CommentStatus.DELETED);
		assertThat(deletedComment.getDeletedAt()).isNotNull();

		mockMvc.perform(get("/api/posts/{postId}/comments", post.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	@Test
	void deleteCommentRejectsRequesterWhoIsNotAuthor() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		User other = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "other@test.com", "encoded-password", "other"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		Comment comment = commentRepository.save(Comment.create(post, author, "comment"));

		mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
				.header("Authorization", "Bearer " + accessToken(other)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("C002"));
	}

	@Test
	void createCommentRejectsBlankContent() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		CommentCreateRequest request = new CommentCreateRequest(" ");

		mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));

		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void createCommentRejectsTooLongContent() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		CommentCreateRequest request = new CommentCreateRequest("a".repeat(1001));

		mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
				.header("Authorization", "Bearer " + accessToken(author))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("G001"));

		assertThat(notificationRepository.count()).isZero();
	}

	@Test
	void getCommentsShowsDeletedUserNickname() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "deleted-author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		commentRepository.save(Comment.create(post, author, "comment"));
		author.softDelete();
		userRepository.saveAndFlush(author);

		mockMvc.perform(get("/api/posts/{postId}/comments", post.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].authorNickname").value("탈퇴한 사용자"));
	}

	@Test
	void getCommentsDoesNotExposeInternalFields() throws Exception {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "title", "content", author));
		commentRepository.save(Comment.create(post, author, "comment"));

		mockMvc.perform(get("/api/posts/{postId}/comments", post.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].authorId").doesNotExist())
			.andExpect(jsonPath("$.data.content[0].post").doesNotExist())
			.andExpect(jsonPath("$.data.content[0].status").doesNotExist())
			.andExpect(jsonPath("$.data.content[0].deletedAt").doesNotExist())
			.andExpect(jsonPath("$.data.content[0].email").doesNotExist())
			.andExpect(jsonPath("$.data.content[0].role").doesNotExist());
	}

	private void deleteAll() {
		notificationRepository.deleteAll();
		teamApplicationRepository.deleteAll();
		userCategoryMembershipRepository.deleteAll();
		teamMemberRepository.deleteAll();
		teamRepository.deleteAll();
		commentRepository.deleteAll();
		postRepository.deleteAll();
		userRepository.deleteAll();
	}

	private String accessToken(User user) {
		return jwtTokenProvider.createAccessToken(user.getId(), UserRole.USER);
	}
}
