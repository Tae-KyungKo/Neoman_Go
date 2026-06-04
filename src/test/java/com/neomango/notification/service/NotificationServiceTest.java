package com.neomango.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neomango.notification.entity.Notification;
import com.neomango.notification.entity.NotificationTargetType;
import com.neomango.notification.entity.NotificationType;
import com.neomango.notification.repository.NotificationRepository;
import com.neomango.user.entity.User;
import com.neomango.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private NotificationService notificationService;

	@Test
	void createTeamApplicationCreatedNotificationSavesNotification() {
		User receiver = User.create("owner@test.com", "encoded-password", "owner");
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
		User receiver = User.create("applicant@test.com", "encoded-password", "applicant");
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
		User receiver = User.create("applicant@test.com", "encoded-password", "applicant");
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
	void createNotificationSkipsSelfAction() {
		notificationService.createTeamApplicationApprovedNotification(1L, 1L, "Futsal Team", 10L);

		verify(userRepository, never()).getReferenceById(1L);
		verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any(Notification.class));
	}
}
