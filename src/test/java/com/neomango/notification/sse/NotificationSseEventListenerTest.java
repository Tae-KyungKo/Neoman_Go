package com.neomango.notification.sse;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.neomango.notification.dto.NotificationResponse;
import com.neomango.notification.entity.Notification;
import com.neomango.notification.entity.NotificationTargetType;
import com.neomango.notification.entity.NotificationType;
import com.neomango.notification.event.NotificationCreatedEvent;
import com.neomango.notification.repository.NotificationRepository;
import com.neomango.user.entity.User;

@ExtendWith(MockitoExtension.class)
class NotificationSseEventListenerTest {

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private NotificationSseService notificationSseService;

	@InjectMocks
	private NotificationSseEventListener notificationSseEventListener;

	@Test
	void handleSendsNotificationSseWhenNotificationExists() {
		Notification notification = notification(10L, 1L);
		when(notificationRepository.findByIdAndReceiverId(10L, 1L)).thenReturn(Optional.of(notification));

		notificationSseEventListener.handle(new NotificationCreatedEvent(10L, 1L));

		verify(notificationSseService).sendToUser(
			eq(1L),
			argThat(response -> response.id().equals(10L)
				&& response.type() == NotificationType.TEAM_APPLICATION_APPROVED
				&& response.targetType() == NotificationTargetType.TEAM_APPLICATION
				&& response.targetId().equals(20L))
		);
	}

	@Test
	void handleDoesNotSendWhenNotificationDoesNotExist() {
		when(notificationRepository.findByIdAndReceiverId(10L, 1L)).thenReturn(Optional.empty());

		notificationSseEventListener.handle(new NotificationCreatedEvent(10L, 1L));

		verify(notificationSseService, never()).sendToUser(any(), any());
	}

	@Test
	void handleDoesNotSendWhenReceiverIdDoesNotMatch() {
		when(notificationRepository.findByIdAndReceiverId(10L, 2L)).thenReturn(Optional.empty());

		notificationSseEventListener.handle(new NotificationCreatedEvent(10L, 2L));

		verify(notificationSseService, never()).sendToUser(any(), any());
	}

	@Test
	void handleDoesNotPropagateSendFailure() {
		Notification notification = notification(10L, 1L);
		when(notificationRepository.findByIdAndReceiverId(10L, 1L)).thenReturn(Optional.of(notification));
		when(notificationSseService.sendToUser(eq(1L), any(NotificationResponse.class)))
			.thenThrow(new IllegalStateException("SSE send failed"));

		assertThatCode(() -> notificationSseEventListener.handle(new NotificationCreatedEvent(10L, 1L)))
			.doesNotThrowAnyException();
	}

	@Test
	void handleUsesAfterCommitWithoutFallbackExecution() throws NoSuchMethodException {
		TransactionalEventListener annotation = NotificationSseEventListener.class
			.getMethod("handle", NotificationCreatedEvent.class)
			.getAnnotation(TransactionalEventListener.class);

		org.assertj.core.api.Assertions.assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
		org.assertj.core.api.Assertions.assertThat(annotation.fallbackExecution()).isFalse();
	}

	private static Notification notification(Long notificationId, Long receiverId) {
		User receiver = User.create(com.neomango.support.TestLoginIds.next(), "receiver" + receiverId + "@test.com", "encoded-password", "receiver" + receiverId);
		ReflectionTestUtils.setField(receiver, "id", receiverId);

		Notification notification = Notification.create(
			receiver,
			NotificationType.TEAM_APPLICATION_APPROVED,
			"가입 신청 승인",
			"테스트 팀 가입 신청이 승인되었습니다.",
			NotificationTargetType.TEAM_APPLICATION,
			20L
		);
		ReflectionTestUtils.setField(notification, "id", notificationId);
		return notification;
	}
}
