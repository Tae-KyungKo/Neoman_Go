package com.neomango.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.neomango.notification.entity.Notification;
import com.neomango.notification.entity.NotificationTargetType;
import com.neomango.notification.entity.NotificationType;
import com.neomango.user.entity.User;
import com.neomango.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

@ActiveProfiles("test")
@DataJpaTest
class NotificationRepositoryTest {

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("save Notification")
	void saveNotification() {
		User receiver = saveUser("receiver-save@test.com", "receiverSave");
		Notification notification = createNotification(receiver);

		Notification savedNotification = notificationRepository.saveAndFlush(notification);

		assertThat(savedNotification.getId()).isNotNull();
		assertThat(savedNotification.getReceiver().getId()).isEqualTo(receiver.getId());
		assertThat(savedNotification.getType()).isEqualTo(NotificationType.TEAM_APPLICATION_CREATED);
		assertThat(savedNotification.getTitle()).isEqualTo("New team application");
		assertThat(savedNotification.getMessage()).isEqualTo("A user applied to your team.");
		assertThat(savedNotification.getTargetType()).isEqualTo(NotificationTargetType.TEAM_APPLICATION);
		assertThat(savedNotification.getTargetId()).isEqualTo(1L);
		assertThat(savedNotification.getReadAt()).isNull();
		assertThat(savedNotification.getCreatedAt()).isNotNull();
		assertThat(savedNotification.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("find notifications by receiver id")
	void findByReceiverId() {
		User receiver = saveUser("receiver-find@test.com", "receiverFind");
		Notification notification = notificationRepository.saveAndFlush(createNotification(receiver));

		Page<Notification> result = notificationRepository.findByReceiverId(receiver.getId(), PageRequest.of(0, 10));

		assertThat(result.getContent()).extracting(Notification::getId)
			.containsExactly(notification.getId());
	}

	@Test
	@DisplayName("findByReceiverId excludes other receiver notifications")
	void findByReceiverIdExcludesOtherReceiverNotifications() {
		User receiver = saveUser("receiver-only@test.com", "receiverOnly");
		User otherReceiver = saveUser("receiver-other@test.com", "receiverOther");
		Notification receiverNotification = notificationRepository.saveAndFlush(createNotification(receiver));
		notificationRepository.saveAndFlush(createNotification(otherReceiver));

		Page<Notification> result = notificationRepository.findByReceiverId(receiver.getId(), PageRequest.of(0, 10));

		assertThat(result.getContent()).extracting(Notification::getId)
			.containsExactly(receiverNotification.getId());
	}

	@Test
	@DisplayName("count unread notifications by receiver id")
	void countByReceiverIdAndReadAtIsNull() {
		User receiver = saveUser("receiver-count@test.com", "receiverCount");
		User otherReceiver = saveUser("receiver-count-other@test.com", "receiverCountOther");
		Notification unreadNotification = notificationRepository.saveAndFlush(createNotification(receiver));
		Notification readNotification = createNotification(receiver);
		readNotification.markAsRead();
		notificationRepository.saveAndFlush(readNotification);
		notificationRepository.saveAndFlush(createNotification(otherReceiver));

		long unreadCount = notificationRepository.countByReceiverIdAndReadAtIsNull(receiver.getId());

		assertThat(unreadNotification.getReadAt()).isNull();
		assertThat(unreadCount).isEqualTo(1);
	}

	@Test
	@DisplayName("markAsRead sets readAt")
	void markAsReadSetsReadAt() {
		User receiver = saveUser("receiver-read@test.com", "receiverRead");
		Notification notification = notificationRepository.saveAndFlush(createNotification(receiver));

		notification.markAsRead();
		notificationRepository.flush();

		assertThat(notification.getReadAt()).isNotNull();
		assertThat(notification.isRead()).isTrue();
	}

	@Test
	@DisplayName("markAsRead keeps existing readAt")
	void markAsReadKeepsExistingReadAt() {
		User receiver = saveUser("receiver-read-again@test.com", "receiverReadAgain");
		Notification notification = notificationRepository.saveAndFlush(createNotification(receiver));
		notification.markAsRead();
		LocalDateTime firstReadAt = notification.getReadAt();

		notification.markAsRead();

		assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
	}

	@Test
	@DisplayName("findByIdAndReceiverId returns own notification only")
	void findByIdAndReceiverIdReturnsOwnNotificationOnly() {
		User receiver = saveUser("receiver-own@test.com", "receiverOwn");
		User otherReceiver = saveUser("receiver-not-own@test.com", "receiverNotOwn");
		Notification notification = notificationRepository.saveAndFlush(createNotification(receiver));

		assertThat(notificationRepository.findByIdAndReceiverId(notification.getId(), receiver.getId()))
			.isPresent();
		assertThat(notificationRepository.findByIdAndReceiverId(notification.getId(), otherReceiver.getId()))
			.isEmpty();
	}

	@Test
	@DisplayName("type and targetType are stored as enum names")
	void typeAndTargetTypeAreStoredAsEnumNames() {
		User receiver = saveUser("receiver-enum@test.com", "receiverEnum");
		Notification notification = notificationRepository.saveAndFlush(createNotification(receiver));
		entityManager.clear();

		@SuppressWarnings("unchecked")
		List<Object[]> rows = entityManager
			.createNativeQuery(
				"select type, target_type from notifications where id = :notificationId"
			)
			.setParameter("notificationId", notification.getId())
			.getResultList();

		assertThat(rows).hasSize(1);
		assertThat(rows.get(0)[0]).isEqualTo(NotificationType.TEAM_APPLICATION_CREATED.name());
		assertThat(rows.get(0)[1]).isEqualTo(NotificationTargetType.TEAM_APPLICATION.name());
	}

	private User saveUser(String email, String nickname) {
		return userRepository.saveAndFlush(User.create(email, "encoded-password", nickname));
	}

	private Notification createNotification(User receiver) {
		return Notification.create(
			receiver,
			NotificationType.TEAM_APPLICATION_CREATED,
			"New team application",
			"A user applied to your team.",
			NotificationTargetType.TEAM_APPLICATION,
			1L
		);
	}
}
