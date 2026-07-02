package com.neomango.notification.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import com.neomango.comment.dto.CommentCreateRequest;
import com.neomango.comment.repository.CommentRepository;
import com.neomango.comment.service.CommentService;
import com.neomango.notification.dto.NotificationResponse;
import com.neomango.notification.entity.NotificationType;
import com.neomango.notification.repository.NotificationRepository;
import com.neomango.notification.service.NotificationService;
import com.neomango.post.entity.Post;
import com.neomango.post.repository.PostRepository;
import com.neomango.user.entity.User;
import com.neomango.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
class NotificationSseIntegrationQaTest {

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private CommentService commentService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@MockitoSpyBean
	private NotificationSseService notificationSseService;

	@BeforeEach
	void setUp() {
		deleteAll();
		notificationSseService.clear();
		reset(notificationSseService);
	}

	@AfterEach
	void tearDown() {
		deleteAll();
	}

	@Test
	void commentNotificationIsStoredAndSseIsSentOnlyAfterCommit() {
		User postAuthor = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "post-author@test.com", "encoded-password", "postAuthor"));
		User commentAuthor = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "comment-author@test.com", "encoded-password", "commentAuthor"));
		Post post = postRepository.save(Post.create("GAME", "post title", "content", postAuthor));

		transactionTemplate.executeWithoutResult(status -> {
			commentService.createComment(post.getId(), commentAuthor.getId(), new CommentCreateRequest("comment"));

			assertThat(notificationRepository.count()).isEqualTo(1);
			verify(notificationSseService, never()).sendToUser(any(), any());
		});

		assertThat(notificationRepository.count()).isEqualTo(1);
		verify(notificationSseService).sendToUser(
			eq(postAuthor.getId()),
			argThat(response -> response.type() == NotificationType.POST_COMMENT_CREATED
				&& response.targetId().equals(post.getId()))
		);
	}

	@Test
	void rollbackPreventsNotificationSse() {
		User receiver = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "rollback-receiver@test.com", "encoded-password", "receiver"));

		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
			notificationService.createTeamApplicationApprovedNotification(
				receiver.getId(),
				999L,
				"Rollback Team",
				10L
			);
			throw new IllegalStateException("rollback");
		})).isInstanceOf(IllegalStateException.class);

		assertThat(notificationRepository.count()).isZero();
		verify(notificationSseService, never()).sendToUser(any(), any());
	}

	@Test
	void selfActionDoesNotStoreNotificationOrSendSse() {
		User author = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "self-author@test.com", "encoded-password", "author"));
		Post post = postRepository.save(Post.create("GAME", "self post", "content", author));

		transactionTemplate.executeWithoutResult(status ->
			commentService.createComment(post.getId(), author.getId(), new CommentCreateRequest("self comment"))
		);

		assertThat(notificationRepository.count()).isZero();
		verify(notificationSseService, never()).sendToUser(any(), any());
	}

	@Test
	void disconnectedReceiverKeepsDbNotificationAndSseSendIsNoOp() {
		User receiver = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "offline@test.com", "encoded-password", "offline"));

		transactionTemplate.executeWithoutResult(status ->
			notificationService.createTeamApplicationApprovedNotification(
				receiver.getId(),
				999L,
				"Offline Team",
				10L
			)
		);

		assertThat(notificationRepository.count()).isEqualTo(1);
		assertThat(notificationSseService.getConnectionCount(receiver.getId())).isZero();
		assertThat(notificationSseService.getTotalConnectionCount()).isZero();
		verify(notificationSseService).sendToUser(eq(receiver.getId()), any(NotificationResponse.class));
	}

	@Test
	void sseSendFailureDoesNotRollbackStoredNotification() {
		User receiver = userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), "sse-fail@test.com", "encoded-password", "receiver"));
		doThrow(new IllegalStateException("SSE send failed"))
			.when(notificationSseService)
			.sendToUser(eq(receiver.getId()), any(NotificationResponse.class));

		transactionTemplate.executeWithoutResult(status ->
			notificationService.createTeamApplicationApprovedNotification(
				receiver.getId(),
				999L,
				"Failure Team",
				10L
			)
		);

		assertThat(notificationRepository.count()).isEqualTo(1);
		verify(notificationSseService).sendToUser(eq(receiver.getId()), any(NotificationResponse.class));
	}

	private void deleteAll() {
		notificationRepository.deleteAll();
		commentRepository.deleteAll();
		postRepository.deleteAll();
		userRepository.deleteAll();
	}
}
