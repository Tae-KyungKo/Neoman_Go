package com.neomango.notification.sse;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import com.neomango.notification.dto.NotificationResponse;
import com.neomango.notification.entity.Notification;
import com.neomango.notification.entity.NotificationTargetType;
import com.neomango.notification.entity.NotificationType;
import com.neomango.notification.event.NotificationCreatedEvent;
import com.neomango.notification.repository.NotificationRepository;
import com.neomango.user.entity.User;
import com.neomango.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
class NotificationSseAfterCommitIntegrationTest {

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private UserRepository userRepository;

	@MockitoBean
	private NotificationSseService notificationSseService;

	@BeforeEach
	void setUp() {
		deleteAll();
		reset(notificationSseService);
	}

	@AfterEach
	void tearDown() {
		deleteAll();
	}

	@Test
	void sendsSseOnlyAfterTransactionCommit() {
		Long[] ids = new Long[2];

		transactionTemplate.executeWithoutResult(status -> {
			User receiver = userRepository.save(User.create("commit@test.com", "encoded-password", "commitUser"));
			Notification notification = notificationRepository.save(createNotification(receiver));

			ids[0] = receiver.getId();
			ids[1] = notification.getId();
			eventPublisher.publishEvent(new NotificationCreatedEvent(notification.getId(), receiver.getId()));

			verifyNoInteractions(notificationSseService);
		});

		verify(notificationSseService).sendToUser(eq(ids[0]), any(NotificationResponse.class));
	}

	@Test
	void doesNotSendSseWhenTransactionRollsBack() {
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
			User receiver = userRepository.save(User.create("rollback@test.com", "encoded-password", "rollbackUser"));
			Notification notification = notificationRepository.save(createNotification(receiver));

			eventPublisher.publishEvent(new NotificationCreatedEvent(notification.getId(), receiver.getId()));
			throw new IllegalStateException("rollback");
		})).isInstanceOf(IllegalStateException.class);

		verify(notificationSseService, never()).sendToUser(any(), any());
	}

	private void deleteAll() {
		notificationRepository.deleteAll();
		userRepository.deleteAll();
	}

	private static Notification createNotification(User receiver) {
		return Notification.create(
			receiver,
			NotificationType.TEAM_APPLICATION_APPROVED,
			"가입 신청 승인",
			"테스트 팀 가입 신청이 승인되었습니다.",
			NotificationTargetType.TEAM_APPLICATION,
			20L
		);
	}
}
