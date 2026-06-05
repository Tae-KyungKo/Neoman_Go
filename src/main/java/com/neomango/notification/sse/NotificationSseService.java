package com.neomango.notification.sse;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.neomango.notification.dto.NotificationResponse;

@Service
public class NotificationSseService {

	private static final long SSE_TIMEOUT_MILLIS = 60L * 60L * 1000L;
	private static final String CONNECTED_EVENT_NAME = "connected";
	private static final String CONNECTED_EVENT_DATA = "connected";
	private static final String NOTIFICATION_EVENT_NAME = "notification";

	private final ConcurrentHashMap<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

	public SseEmitter connect(Long userId) {
		SseEmitter emitter = createEmitter();
		register(userId, emitter);
		sendConnectedEvent(userId, emitter);

		return emitter;
	}

	public int sendToUser(Long receiverId, NotificationResponse response) {
		Set<SseEmitter> userEmitters = emitters.get(receiverId);
		if (userEmitters == null || userEmitters.isEmpty()) {
			return 0;
		}

		int successCount = 0;
		for (SseEmitter emitter : userEmitters) {
			try {
				emitter.send(SseEmitter.event()
					.name(NOTIFICATION_EVENT_NAME)
					.data(response));
				successCount++;
			} catch (IOException | RuntimeException e) {
				remove(receiverId, emitter);
			}
		}

		return successCount;
	}

	SseEmitter createEmitter() {
		return new SseEmitter(SSE_TIMEOUT_MILLIS);
	}

	private void register(Long userId, SseEmitter emitter) {
		emitters.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);

		emitter.onCompletion(() -> remove(userId, emitter));
		emitter.onTimeout(() -> remove(userId, emitter));
		emitter.onError(ignored -> remove(userId, emitter));
	}

	private void sendConnectedEvent(Long userId, SseEmitter emitter) {
		try {
			emitter.send(SseEmitter.event()
				.name(CONNECTED_EVENT_NAME)
				.data(CONNECTED_EVENT_DATA));
		} catch (IOException | RuntimeException e) {
			remove(userId, emitter);
		}
	}

	void remove(Long userId, SseEmitter emitter) {
		Set<SseEmitter> userEmitters = emitters.get(userId);
		if (userEmitters == null) {
			return;
		}

		userEmitters.remove(emitter);
		if (userEmitters.isEmpty()) {
			emitters.remove(userId, userEmitters);
		}
	}

	int getConnectionCount(Long userId) {
		Set<SseEmitter> userEmitters = emitters.get(userId);
		if (userEmitters == null) {
			return 0;
		}

		return userEmitters.size();
	}

	int getTotalConnectionCount() {
		return emitters.values().stream()
			.mapToInt(Set::size)
			.sum();
	}

	void clear() {
		emitters.clear();
	}
}
