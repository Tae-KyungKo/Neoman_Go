package com.neomango.notification.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

	private static class TestNotificationSseService extends NotificationSseService {

		private final SseEmitter emitter;

		private TestNotificationSseService(SseEmitter emitter) {
			this.emitter = emitter;
		}

		@Override
		SseEmitter createEmitter() {
			return emitter;
		}
	}

	private static class CapturingSseEmitter extends SseEmitter {

		private Runnable completionCallback;

		private CapturingSseEmitter() {
			super(60L * 60L * 1000L);
		}

		@Override
		public synchronized void onCompletion(Runnable callback) {
			this.completionCallback = callback;
			super.onCompletion(callback);
		}

		private void runCompletionCallback() {
			completionCallback.run();
		}
	}
}
