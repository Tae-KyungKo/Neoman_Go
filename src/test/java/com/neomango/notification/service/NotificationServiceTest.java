package com.neomango.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neomango.notification.entity.Notification;
import com.neomango.notification.entity.NotificationTargetType;
import com.neomango.notification.entity.NotificationType;
import com.neomango.notification.event.NotificationCreatedEvent;
import com.neomango.notification.repository.NotificationRepository;
import com.neomango.user.entity.User;
import com.neomango.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private NotificationService notificationService;

	@BeforeEach
	void setUp() {
		lenient().when(notificationRepository.save(any(Notification.class)))
			.thenAnswer(invocation -> {
				Notification notification = invocation.getArgument(0);
				ReflectionTestUtils.setField(notification, "id", 100L);
				return notification;
			});
	}

	@Test
	void createTeamApplicationCreatedNotificationSavesNotification() {
		User receiver = User.create(com.neomango.support.TestLoginIds.next(), "owner@test.com", "encoded-password", "owner");
		when(userRepository.getReferenceById(1L)).thenReturn(receiver);

		notificationService.createTeamApplicationCreatedNotification(
			1L,
			2L,
			"Futsal Team",
			"applicant",
			10L
		);

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(captor.capture());
		Notification notification = captor.getValue();
		assertThat(notification.getReceiver()).isSameAs(receiver);
		assertThat(notification.getType()).isEqualTo(NotificationType.TEAM_APPLICATION_CREATED);
		assertThat(notification.getTitle()).isEqualTo("팀 가입 신청");
		assertThat(notification.getMessage()).isEqualTo("applicant님이 Futsal Team 팀에 가입 신청했습니다.");
		assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.TEAM_APPLICATION);
		assertThat(notification.getTargetId()).isEqualTo(10L);
	}

	@Test
	void createTeamApplicationApprovedNotificationSavesNotification() {
		User receiver = User.create(com.neomango.support.TestLoginIds.next(), "applicant@test.com", "encoded-password", "applicant");
		when(userRepository.getReferenceById(2L)).thenReturn(receiver);

		notificationService.createTeamApplicationApprovedNotification(2L, 1L, "Futsal Team", 10L);

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(captor.capture());
		Notification notification = captor.getValue();
		assertThat(notification.getReceiver()).isSameAs(receiver);
		assertThat(notification.getType()).isEqualTo(NotificationType.TEAM_APPLICATION_APPROVED);
		assertThat(notification.getTitle()).isEqualTo("가입 신청 승인");
		assertThat(notification.getMessage()).isEqualTo("Futsal Team 팀 가입 신청이 승인되었습니다.");
		assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.TEAM_APPLICATION);
		assertThat(notification.getTargetId()).isEqualTo(10L);
	}

	@Test
	void createTeamApplicationRejectedNotificationSavesNotification() {
		User receiver = User.create(com.neomango.support.TestLoginIds.next(), "applicant@test.com", "encoded-password", "applicant");
		when(userRepository.getReferenceById(2L)).thenReturn(receiver);

		notificationService.createTeamApplicationRejectedNotification(2L, 1L, "Futsal Team", 10L);

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(captor.capture());
		Notification notification = captor.getValue();
		assertThat(notification.getReceiver()).isSameAs(receiver);
		assertThat(notification.getType()).isEqualTo(NotificationType.TEAM_APPLICATION_REJECTED);
		assertThat(notification.getTitle()).isEqualTo("가입 신청 거절");
		assertThat(notification.getMessage()).isEqualTo("Futsal Team 팀 가입 신청이 거절되었습니다.");
		assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.TEAM_APPLICATION);
		assertThat(notification.getTargetId()).isEqualTo(10L);
	}

	@Test
	void createTeamMemberJoinedNotificationSavesNotification() {
		User receiver = User.create(com.neomango.support.TestLoginIds.next(), "member@test.com", "encoded-password", "member");
		when(userRepository.getReferenceById(3L)).thenReturn(receiver);

		notificationService.createTeamMemberJoinedNotification(3L, 2L, "Futsal Team", "joinedMember", 20L);

		assertSavedNotification(
			receiver,
			NotificationType.TEAM_MEMBER_JOINED,
			"새 멤버 가입",
			"joinedMember님이 Futsal Team 팀에 합류했습니다.",
			NotificationTargetType.TEAM,
			20L
		);
	}

	@Test
	void createTeamMemberLeftNotificationSavesNotification() {
		User receiver = User.create(com.neomango.support.TestLoginIds.next(), "member@test.com", "encoded-password", "member");
		when(userRepository.getReferenceById(3L)).thenReturn(receiver);

		notificationService.createTeamMemberLeftNotification(3L, 2L, "Futsal Team", "leftMember", 20L);

		assertSavedNotification(
			receiver,
			NotificationType.TEAM_MEMBER_LEFT,
			"팀원 탈퇴",
			"leftMember님이 Futsal Team 팀에서 탈퇴했습니다.",
			NotificationTargetType.TEAM,
			20L
		);
	}

	@Test
	void createTeamMemberKickedNotificationSavesNotification() {
		User receiver = User.create(com.neomango.support.TestLoginIds.next(), "member@test.com", "encoded-password", "member");
		when(userRepository.getReferenceById(3L)).thenReturn(receiver);

		notificationService.createTeamMemberKickedNotification(3L, 1L, "Futsal Team", 20L);

		assertSavedNotification(
			receiver,
			NotificationType.TEAM_MEMBER_KICKED,
			"팀원 강퇴",
			"Futsal Team 팀에서 강퇴되었습니다.",
			NotificationTargetType.TEAM,
			20L
		);
	}

	@Test
	void createTeamOwnerDelegatedNotificationSavesNotification() {
		User receiver = User.create(com.neomango.support.TestLoginIds.next(), "new-owner@test.com", "encoded-password", "newOwner");
		when(userRepository.getReferenceById(3L)).thenReturn(receiver);

		notificationService.createTeamOwnerDelegatedNotification(3L, 1L, "Futsal Team", 20L);

		assertSavedNotification(
			receiver,
			NotificationType.TEAM_OWNER_DELEGATED,
			"주장 권한 위임",
			"Futsal Team 팀의 주장 권한을 위임받았습니다.",
			NotificationTargetType.TEAM,
			20L
		);
	}

	@Test
	void createPostCommentCreatedNotificationSavesNotification() {
		User receiver = User.create(com.neomango.support.TestLoginIds.next(), "post-author@test.com", "encoded-password", "postAuthor");
		when(userRepository.getReferenceById(4L)).thenReturn(receiver);

		notificationService.createPostCommentCreatedNotification(4L, 5L, "Post Title", "commentAuthor", 30L);

		assertSavedNotification(
			receiver,
			NotificationType.POST_COMMENT_CREATED,
			"새 댓글",
			"commentAuthor님이 \"Post Title\" 게시글에 댓글을 작성했습니다.",
			NotificationTargetType.POST,
			30L
		);
	}

	@Test
	void createPostCommentCreatedNotificationSkipsSelfAction() {
		notificationService.createPostCommentCreatedNotification(1L, 1L, "Post Title", "author", 30L);

		verify(userRepository, never()).getReferenceById(1L);
		verify(notificationRepository, never()).save(any(Notification.class));
		verify(eventPublisher, never()).publishEvent(any(NotificationCreatedEvent.class));
	}

	@Test
	void createNotificationSkipsSelfAction() {
		notificationService.createTeamApplicationApprovedNotification(1L, 1L, "Futsal Team", 10L);

		verify(userRepository, never()).getReferenceById(1L);
		verify(notificationRepository, never()).save(any(Notification.class));
		verify(eventPublisher, never()).publishEvent(any(NotificationCreatedEvent.class));
	}

	@Test
	void createTeamMemberJoinedNotificationSkipsSelfAction() {
		notificationService.createTeamMemberJoinedNotification(1L, 1L, "Futsal Team", "joinedMember", 20L);

		verify(userRepository, never()).getReferenceById(1L);
		verify(notificationRepository, never()).save(any(Notification.class));
		verify(eventPublisher, never()).publishEvent(any(NotificationCreatedEvent.class));
	}

	@Test
	void createTeamMemberLeftNotificationSkipsSelfAction() {
		notificationService.createTeamMemberLeftNotification(1L, 1L, "Futsal Team", "leftMember", 20L);

		verify(userRepository, never()).getReferenceById(1L);
		verify(notificationRepository, never()).save(any(Notification.class));
		verify(eventPublisher, never()).publishEvent(any(NotificationCreatedEvent.class));
	}

	@Test
	void createTeamMemberKickedNotificationSkipsSelfAction() {
		notificationService.createTeamMemberKickedNotification(1L, 1L, "Futsal Team", 20L);

		verify(userRepository, never()).getReferenceById(1L);
		verify(notificationRepository, never()).save(any(Notification.class));
		verify(eventPublisher, never()).publishEvent(any(NotificationCreatedEvent.class));
	}

	@Test
	void createTeamOwnerDelegatedNotificationSkipsSelfAction() {
		notificationService.createTeamOwnerDelegatedNotification(1L, 1L, "Futsal Team", 20L);

		verify(userRepository, never()).getReferenceById(1L);
		verify(notificationRepository, never()).save(any(Notification.class));
		verify(eventPublisher, never()).publishEvent(any(NotificationCreatedEvent.class));
	}

	@Test
	void createNotificationPublishesNotificationCreatedEvent() {
		User receiver = User.create(com.neomango.support.TestLoginIds.next(), "applicant@test.com", "encoded-password", "applicant");
		when(userRepository.getReferenceById(2L)).thenReturn(receiver);

		notificationService.createTeamApplicationApprovedNotification(2L, 1L, "Futsal Team", 10L);

		ArgumentCaptor<NotificationCreatedEvent> eventCaptor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		NotificationCreatedEvent event = eventCaptor.getValue();
		assertThat(event.notificationId()).isEqualTo(100L);
		assertThat(event.receiverId()).isEqualTo(2L);
	}

	private void assertSavedNotification(
		User receiver,
		NotificationType type,
		String title,
		String message,
		NotificationTargetType targetType,
		Long targetId
	) {
		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(captor.capture());
		Notification notification = captor.getValue();
		assertThat(notification.getReceiver()).isSameAs(receiver);
		assertThat(notification.getType()).isEqualTo(type);
		assertThat(notification.getTitle()).isEqualTo(title);
		assertThat(notification.getMessage()).isEqualTo(message);
		assertThat(notification.getTargetType()).isEqualTo(targetType);
		assertThat(notification.getTargetId()).isEqualTo(targetId);
	}
}
