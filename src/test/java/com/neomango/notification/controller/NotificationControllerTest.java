package com.neomango.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.neomango.auth.jwt.JwtTokenProvider;
import com.neomango.notification.entity.Notification;
import com.neomango.notification.entity.NotificationTargetType;
import com.neomango.notification.entity.NotificationType;
import com.neomango.notification.repository.NotificationRepository;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserRole;
import com.neomango.user.repository.UserRepository;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private NotificationRepository notificationRepository;

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
	void getMyNotificationsReturnsAuthenticatedUserNotifications() throws Exception {
		User user = saveUser("notification-user@test.com", "notificationUser");
		Notification notification = notificationRepository.save(createNotification(user, "title", 1L));
		setCreatedAt(notification, LocalDateTime.of(2026, 7, 1, 22, 30));
		notificationRepository.saveAndFlush(notification);

		mockMvc.perform(get("/api/notifications")
				.header("Authorization", "Bearer " + accessToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.content[0].id").value(notification.getId()))
			.andExpect(jsonPath("$.data.content[0].type").value("TEAM_APPLICATION_CREATED"))
			.andExpect(jsonPath("$.data.content[0].title").value("title"))
			.andExpect(jsonPath("$.data.content[0].message").value("A user applied to your team."))
			.andExpect(jsonPath("$.data.content[0].targetType").value("TEAM_APPLICATION"))
			.andExpect(jsonPath("$.data.content[0].targetId").value(1L))
			.andExpect(jsonPath("$.data.content[0].read").value(false))
			.andExpect(jsonPath("$.data.content[0].readAt").doesNotExist())
			.andExpect(jsonPath("$.data.content[0].createdAt").value(org.hamcrest.Matchers.endsWith("+09:00")));
	}

	@Test
	void getMyNotificationsRejectsUnauthenticatedUser() throws Exception {
		mockMvc.perform(get("/api/notifications"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void getMyNotificationsExcludesOtherUserNotifications() throws Exception {
		User user = saveUser("notification-owner@test.com", "notificationOwner");
		User otherUser = saveUser("notification-other@test.com", "notificationOther");
		Notification ownNotification = notificationRepository.save(createNotification(user, "own", 1L));
		notificationRepository.save(createNotification(otherUser, "other", 2L));

		mockMvc.perform(get("/api/notifications")
				.header("Authorization", "Bearer " + accessToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content.length()").value(1))
			.andExpect(jsonPath("$.data.content[0].id").value(ownNotification.getId()))
			.andExpect(jsonPath("$.data.content[0].title").value("own"));
	}

	@Test
	void getMyNotificationsReturnsLatestOrderByCreatedAtDescAndIdDesc() throws Exception {
		User user = saveUser("notification-sort@test.com", "notificationSort");
		Notification olderNotification = notificationRepository.save(createNotification(user, "older", 1L));
		Notification lowerIdNotification = notificationRepository.save(createNotification(user, "lower id", 2L));
		Notification higherIdNotification = notificationRepository.save(createNotification(user, "higher id", 3L));

		LocalDateTime olderCreatedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
		LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 1, 2, 0, 0);
		setCreatedAt(olderNotification, olderCreatedAt);
		setCreatedAt(lowerIdNotification, sameCreatedAt);
		setCreatedAt(higherIdNotification, sameCreatedAt);
		notificationRepository.saveAndFlush(olderNotification);
		notificationRepository.saveAndFlush(lowerIdNotification);
		notificationRepository.saveAndFlush(higherIdNotification);

		mockMvc.perform(get("/api/notifications")
				.header("Authorization", "Bearer " + accessToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0].id").value(higherIdNotification.getId()))
			.andExpect(jsonPath("$.data.content[1].id").value(lowerIdNotification.getId()))
			.andExpect(jsonPath("$.data.content[2].id").value(olderNotification.getId()));
	}

	@Test
	void getMyNotificationsDoesNotExposeReceiverFields() throws Exception {
		User user = saveUser("notification-private@test.com", "notificationPrivate");
		notificationRepository.save(createNotification(user, "private", 1L));

		mockMvc.perform(get("/api/notifications")
				.header("Authorization", "Bearer " + accessToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.content[0]", not(hasKey("receiverId"))))
			.andExpect(jsonPath("$.data.content[0]", not(hasKey("receiver"))))
			.andExpect(jsonPath("$.data.content[0]", not(hasKey("user"))))
			.andExpect(jsonPath("$.data.content[0]", not(hasKey("email"))))
			.andExpect(jsonPath("$.data.content[0]", not(hasKey("nickname"))));
	}

	@Test
	void getUnreadCountReturnsUnreadOwnNotificationsOnly() throws Exception {
		User user = saveUser("notification-count@test.com", "notificationCount");
		User otherUser = saveUser("notification-count-other@test.com", "notificationCountOther");
		notificationRepository.save(createNotification(user, "unread", 1L));
		Notification readNotification = createNotification(user, "read", 2L);
		readNotification.markAsRead();
		notificationRepository.save(readNotification);
		notificationRepository.save(createNotification(otherUser, "other unread", 3L));

		mockMvc.perform(get("/api/notifications/unread-count")
				.header("Authorization", "Bearer " + accessToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.unreadCount").value(1));
	}

	@Test
	void getUnreadCountRejectsUnauthenticatedUser() throws Exception {
		mockMvc.perform(get("/api/notifications/unread-count"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void markAsReadMarksOwnNotification() throws Exception {
		User user = saveUser("notification-read@test.com", "notificationRead");
		Notification notification = notificationRepository.save(createNotification(user, "read", 1L));

		mockMvc.perform(patch("/api/notifications/{notificationId}/read", notification.getId())
				.header("Authorization", "Bearer " + accessToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		Notification readNotification = notificationRepository.findById(notification.getId()).orElseThrow();
		assertThat(readNotification.getReadAt()).isNotNull();
		assertThat(readNotification.isRead()).isTrue();
	}

	@Test
	void markAsReadRejectsUnauthenticatedUser() throws Exception {
		mockMvc.perform(patch("/api/notifications/{notificationId}/read", 1L))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void markAsReadIsIdempotent() throws Exception {
		User user = saveUser("notification-read-again@test.com", "notificationReadAgain");
		Notification notification = createNotification(user, "read again", 1L);
		LocalDateTime firstReadAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0, 123456000);
		setReadAt(notification, firstReadAt);
		notificationRepository.saveAndFlush(notification);

		mockMvc.perform(patch("/api/notifications/{notificationId}/read", notification.getId())
				.header("Authorization", "Bearer " + accessToken(user)))
			.andExpect(status().isOk());

		Notification readNotification = notificationRepository.findById(notification.getId()).orElseThrow();
		assertThat(readNotification.getReadAt()).isEqualTo(firstReadAt);
	}

	@Test
	void markAsReadRejectsOtherUserNotification() throws Exception {
		User user = saveUser("notification-reader@test.com", "notificationReader");
		User otherUser = saveUser("notification-owner-other@test.com", "notificationOwnerOther");
		Notification notification = notificationRepository.save(createNotification(otherUser, "other", 1L));

		mockMvc.perform(patch("/api/notifications/{notificationId}/read", notification.getId())
				.header("Authorization", "Bearer " + accessToken(user)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NT001"));

		Notification unreadNotification = notificationRepository.findById(notification.getId()).orElseThrow();
		assertThat(unreadNotification.getReadAt()).isNull();
	}

	@Test
	void markAsReadRejectsUnknownNotification() throws Exception {
		User user = saveUser("notification-unknown@test.com", "notificationUnknown");

		mockMvc.perform(patch("/api/notifications/{notificationId}/read", 999999L)
				.header("Authorization", "Bearer " + accessToken(user)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NT001"))
			.andExpect(jsonPath("$.message").value("존재하지 않는 알림입니다."));
	}

	@Test
	void markAllAsReadMarksOnlyOwnUnreadNotifications() throws Exception {
		User user = saveUser("notification-read-all@test.com", "notificationReadAll");
		User otherUser = saveUser("notification-read-all-other@test.com", "notificationReadAllOther");
		Notification unreadNotification = notificationRepository.save(createNotification(user, "unread", 1L));
		Notification anotherUnreadNotification = notificationRepository.save(createNotification(user, "another unread", 2L));
		Notification readNotification = createNotification(user, "read", 3L);
		LocalDateTime existingReadAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0, 654321000);
		setReadAt(readNotification, existingReadAt);
		notificationRepository.save(readNotification);
		Notification otherUnreadNotification = notificationRepository.save(createNotification(otherUser, "other", 4L));
		notificationRepository.flush();

		mockMvc.perform(patch("/api/notifications/read-all")
				.header("Authorization", "Bearer " + accessToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		Notification firstReadNotification = notificationRepository.findById(unreadNotification.getId()).orElseThrow();
		Notification secondReadNotification = notificationRepository.findById(anotherUnreadNotification.getId()).orElseThrow();
		Notification alreadyReadNotification = notificationRepository.findById(readNotification.getId()).orElseThrow();
		Notification otherNotification = notificationRepository.findById(otherUnreadNotification.getId()).orElseThrow();
		assertThat(firstReadNotification.getReadAt()).isNotNull();
		assertThat(secondReadNotification.getReadAt()).isNotNull();
		assertThat(alreadyReadNotification.getReadAt()).isEqualTo(existingReadAt);
		assertThat(otherNotification.getReadAt()).isNull();
	}

	@Test
	void markAllAsReadRejectsUnauthenticatedUser() throws Exception {
		mockMvc.perform(patch("/api/notifications/read-all"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void markAllAsReadMakesUnreadCountZero() throws Exception {
		User user = saveUser("notification-zero@test.com", "notificationZero");
		notificationRepository.save(createNotification(user, "unread1", 1L));
		notificationRepository.save(createNotification(user, "unread2", 2L));

		mockMvc.perform(patch("/api/notifications/read-all")
				.header("Authorization", "Bearer " + accessToken(user)))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/notifications/unread-count")
				.header("Authorization", "Bearer " + accessToken(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.unreadCount").value(0));
	}

	private void deleteAll() {
		notificationRepository.deleteAll();
		userRepository.deleteAll();
	}

	private User saveUser(String email, String nickname) {
		return userRepository.save(User.create(com.neomango.support.TestLoginIds.next(), email, "encoded-password", nickname));
	}

	private String accessToken(User user) {
		return jwtTokenProvider.createAccessToken(user.getId(), UserRole.USER);
	}

	private Notification createNotification(User receiver, String title, Long targetId) {
		return Notification.create(
			receiver,
			NotificationType.TEAM_APPLICATION_CREATED,
			title,
			"A user applied to your team.",
			NotificationTargetType.TEAM_APPLICATION,
			targetId
		);
	}

	private void setCreatedAt(Notification notification, LocalDateTime createdAt) {
		ReflectionTestUtils.setField(notification, "createdAt", createdAt);
	}

	private void setReadAt(Notification notification, LocalDateTime readAt) {
		ReflectionTestUtils.setField(notification, "readAt", readAt);
	}
}
