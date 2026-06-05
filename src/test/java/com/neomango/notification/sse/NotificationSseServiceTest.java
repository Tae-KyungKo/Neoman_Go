package com.neomango.notification.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.neomango.notification.dto.NotificationResponse;
import com.neomango.notification.entity.NotificationTargetType;
import com.neomango.notification.entity.NotificationType;

class NotificationSseServiceTest {

	private NotificationSseService notificationSseService;

	@BeforeEach
	void setUp() {
		notificationSseService = new NotificationSseService();
	}

	@Test
	void connectRegistersEmitterByUserId() {
		notificationSseService.connect(1L);

		assertThat(notificationSseService.getConnectionCount(1L)).isEqualTo(1);
		assertThat(notificationSseService.getTotalConnectionCount()).isEqualTo(1);
	}

	@Test
	void connectAllowsMultipleEmittersForSameUserId() {
		notificationSseService.connect(1L);
		notificationSseService.connect(1L);

		assertThat(notificationSseService.getConnectionCount(1L)).isEqualTo(2);
		assertThat(notificationSseService.getTotalConnectionCount()).isEqualTo(2);
	}

	@Test
	void connectManagesDifferentUserEmittersIndependently() {
		notificationSseService.connect(1L);
		notificationSseService.connect(2L);

		assertThat(notificationSseService.getConnectionCount(1L)).isEqualTo(1);
		assertThat(notificationSseService.getConnectionCount(2L)).isEqualTo(1);
		assertThat(notificationSseService.getTotalConnectionCount()).isEqualTo(2);
	}

	@Test
	void removeDeletesTargetEmitterOnly() {
		SseEmitter firstEmitter = notificationSseService.connect(1L);
		notificationSseService.connect(1L);

		notificationSseService.remove(1L, firstEmitter);

		assertThat(notificationSseService.getConnectionCount(1L)).isEqualTo(1);
		assertThat(notificationSseService.getTotalConnectionCount()).isEqualTo(1);
	}

	@Test
	void removeDeletesUserKeyWhenAllEmittersRemoved() {
		SseEmitter emitter = notificationSseService.connect(1L);

		notificationSseService.remove(1L, emitter);

		assertThat(notificationSseService.getConnectionCount(1L)).isZero();
		assertThat(notificationSseService.getTotalConnectionCount()).isZero();
	}

	@Test
	void removeIsIdempotent() {
		SseEmitter emitter = notificationSseService.connect(1L);

		notificationSseService.remove(1L, emitter);

		assertThatCode(() -> notificationSseService.remove(1L, emitter))
			.doesNotThrowAnyException();
		assertThat(notificationSseService.getConnectionCount(1L)).isZero();
		assertThat(notificationSseService.getTotalConnectionCount()).isZero();
	}

	@Test
	void completionCleanupRemovesEmitter() {
		CapturingSseEmitter emitter = new CapturingSseEmitter();
		NotificationSseService service = new TestNotificationSseService(emitter);

		service.connect(1L);
		emitter.runCompletionCallback();

		assertThat(service.getConnectionCount(1L)).isZero();
		assertThat(service.getTotalConnectionCount()).isZero();
	}

	@Test
	void sendToUserSendsNotificationEventToConnectedEmitter() {
		CapturingSseEmitter emitter = new CapturingSseEmitter();
		NotificationSseService service = new TestNotificationSseService(emitter);
		NotificationResponse response = notificationResponse();
		service.connect(1L);
		emitter.clearSentEvents();

		int successCount = service.sendToUser(1L, response);

		assertThat(successCount).isEqualTo(1);
		assertThat(emitter.getNotificationEventCount()).isEqualTo(1);
		assertThat(emitter.hasNotificationData(response)).isTrue();
	}

	@Test
	void sendToUserSendsNotificationEventToAllEmittersForSameUserId() {
		CapturingSseEmitter firstEmitter = new CapturingSseEmitter();
		CapturingSseEmitter secondEmitter = new CapturingSseEmitter();
		NotificationSseService service = new TestNotificationSseService(firstEmitter, secondEmitter);
		NotificationResponse response = notificationResponse();
		service.connect(1L);
		service.connect(1L);
		firstEmitter.clearSentEvents();
		secondEmitter.clearSentEvents();

		int successCount = service.sendToUser(1L, response);

		assertThat(successCount).isEqualTo(2);
		assertThat(firstEmitter.getNotificationEventCount()).isEqualTo(1);
		assertThat(secondEmitter.getNotificationEventCount()).isEqualTo(1);
	}

	@Test
	void sendToUserReturnsZeroWhenUserHasNoEmitter() {
		NotificationResponse response = notificationResponse();

		assertThatCode(() -> {
			int successCount = notificationSseService.sendToUser(1L, response);
			assertThat(successCount).isZero();
		}).doesNotThrowAnyException();
	}

	@Test
	void sendToUserDoesNotSendToDifferentUserEmitter() {
		CapturingSseEmitter targetEmitter = new CapturingSseEmitter();
		CapturingSseEmitter otherEmitter = new CapturingSseEmitter();
		NotificationSseService service = new TestNotificationSseService(targetEmitter, otherEmitter);
		NotificationResponse response = notificationResponse();
		service.connect(1L);
		service.connect(2L);
		targetEmitter.clearSentEvents();
		otherEmitter.clearSentEvents();

		int successCount = service.sendToUser(1L, response);

		assertThat(successCount).isEqualTo(1);
		assertThat(targetEmitter.getNotificationEventCount()).isEqualTo(1);
		assertThat(otherEmitter.getNotificationEventCount()).isZero();
	}

	@Test
	void sendToUserCleansUpFailedEmitter() {
		CapturingSseEmitter failedEmitter = CapturingSseEmitter.failingWithIOException();
		NotificationSseService service = new TestNotificationSseService(failedEmitter);
		service.connect(1L);
		failedEmitter.clearSentEvents();

		int successCount = service.sendToUser(1L, notificationResponse());

		assertThat(successCount).isZero();
		assertThat(service.getConnectionCount(1L)).isZero();
		assertThat(service.getTotalConnectionCount()).isZero();
	}

	@Test
	void sendToUserContinuesWhenOneEmitterFails() {
		CapturingSseEmitter failedEmitter = CapturingSseEmitter.failingWithIOException();
		CapturingSseEmitter successEmitter = new CapturingSseEmitter();
		NotificationSseService service = new TestNotificationSseService(failedEmitter, successEmitter);
		NotificationResponse response = notificationResponse();
		service.connect(1L);
		service.connect(1L);
		failedEmitter.clearSentEvents();
		successEmitter.clearSentEvents();

		int successCount = service.sendToUser(1L, response);

		assertThat(successCount).isEqualTo(1);
		assertThat(service.getConnectionCount(1L)).isEqualTo(1);
		assertThat(successEmitter.getNotificationEventCount()).isEqualTo(1);
	}

	@Test
	void sendToUserDoesNotPropagateSendFailure() {
		CapturingSseEmitter failedEmitter = CapturingSseEmitter.failingWithIllegalStateException();
		NotificationSseService service = new TestNotificationSseService(failedEmitter);
		service.connect(1L);
		failedEmitter.clearSentEvents();

		assertThatCode(() -> service.sendToUser(1L, notificationResponse()))
			.doesNotThrowAnyException();
		assertThat(service.getConnectionCount(1L)).isZero();
	}

	@Test
	void sendToUserReturnsSuccessfulEmitterCountOnly() {
		CapturingSseEmitter firstSuccessEmitter = new CapturingSseEmitter();
		CapturingSseEmitter failedEmitter = CapturingSseEmitter.failingWithIOException();
		CapturingSseEmitter secondSuccessEmitter = new CapturingSseEmitter();
		NotificationSseService service = new TestNotificationSseService(
			firstSuccessEmitter,
			failedEmitter,
			secondSuccessEmitter
		);
		service.connect(1L);
		service.connect(1L);
		service.connect(1L);
		firstSuccessEmitter.clearSentEvents();
		failedEmitter.clearSentEvents();
		secondSuccessEmitter.clearSentEvents();

		int successCount = service.sendToUser(1L, notificationResponse());

		assertThat(successCount).isEqualTo(2);
		assertThat(service.getConnectionCount(1L)).isEqualTo(2);
	}

	private static NotificationResponse notificationResponse() {
		return new NotificationResponse(
			1L,
			NotificationType.TEAM_APPLICATION_APPROVED,
			"가입 신청이 승인되었습니다",
			"테스트 팀 가입 신청이 승인되었습니다",
			NotificationTargetType.TEAM_APPLICATION,
			10L,
			false,
			null,
			LocalDateTime.of(2026, 6, 5, 12, 0)
		);
	}

	private static boolean containsEventName(Set<DataWithMediaType> event, String eventName) {
		return event.stream()
			.map(DataWithMediaType::getData)
			.map(String::valueOf)
			.anyMatch(data -> data.contains("event:" + eventName));
	}

	private static boolean containsData(Set<DataWithMediaType> event, Object expectedData) {
		return event.stream()
			.map(DataWithMediaType::getData)
			.anyMatch(expectedData::equals);
	}

	private static class TestNotificationSseService extends NotificationSseService {

		private final Queue<SseEmitter> emitters;

		private TestNotificationSseService(SseEmitter... emitters) {
			this.emitters = new ArrayDeque<>(List.of(emitters));
		}

		@Override
		SseEmitter createEmitter() {
			SseEmitter emitter = emitters.poll();
			if (emitter == null) {
				throw new IllegalStateException("No test emitter remains");
			}
			return emitter;
		}
	}

	private static class CapturingSseEmitter extends SseEmitter {

		private final List<Set<DataWithMediaType>> sentEvents = new ArrayList<>();
		private final boolean failNotificationWithIOException;
		private final boolean failNotificationWithIllegalStateException;
		private Runnable completionCallback;

		private CapturingSseEmitter() {
			this(false, false);
		}

		private CapturingSseEmitter(
			boolean failNotificationWithIOException,
			boolean failNotificationWithIllegalStateException
		) {
			super(60L * 60L * 1000L);
			this.failNotificationWithIOException = failNotificationWithIOException;
			this.failNotificationWithIllegalStateException = failNotificationWithIllegalStateException;
		}

		private static CapturingSseEmitter failingWithIOException() {
			return new CapturingSseEmitter(true, false);
		}

		private static CapturingSseEmitter failingWithIllegalStateException() {
			return new CapturingSseEmitter(false, true);
		}

		@Override
		public synchronized void send(SseEventBuilder builder) throws IOException {
			Set<DataWithMediaType> event = builder.build();
			if (containsEventName(event, "notification")) {
				if (failNotificationWithIOException) {
					throw new IOException("SSE send failed");
				}
				if (failNotificationWithIllegalStateException) {
					throw new IllegalStateException("SSE emitter is already completed");
				}
			}

			sentEvents.add(event);
		}

		@Override
		public synchronized void onCompletion(Runnable callback) {
			this.completionCallback = callback;
			super.onCompletion(callback);
		}

		private void runCompletionCallback() {
			completionCallback.run();
		}

		private void clearSentEvents() {
			sentEvents.clear();
		}

		private int getNotificationEventCount() {
			return (int)sentEvents.stream()
				.filter(event -> containsEventName(event, "notification"))
				.count();
		}

		private boolean hasNotificationData(NotificationResponse response) {
			return sentEvents.stream()
				.filter(event -> containsEventName(event, "notification"))
				.anyMatch(event -> containsData(event, response));
		}
	}
}
