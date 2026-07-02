package com.neomango.notification.sse;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.neomango.notification.dto.NotificationResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationSseService {

	private static final long SSE_TIMEOUT_MILLIS = 60L * 60L * 1000L;
	private static final String CONNECTED_EVENT_NAME = "connected";
	private static final String CONNECTED_EVENT_DATA = "connected";
	private static final String NOTIFICATION_EVENT_NAME = "notification";
	private static final String HEARTBEAT_COMMENT = "heartbeat";

	private final ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

	public SseEmitter connect(Long userId) {
		SseEmitter emitter = createEmitter();
		String connectionId = register(userId, emitter);
		sendConnectedEvent(userId, connectionId, emitter);

		return emitter;
	}

	public int sendToUser(Long receiverId, NotificationResponse response) {
		ConcurrentHashMap<String, SseEmitter> userEmitters = emitters.get(receiverId);
		if (userEmitters == null || userEmitters.isEmpty()) {
			return 0;
		}

		int successCount = 0;
		for (var entry : userEmitters.entrySet()) {
			try {
				entry.getValue().send(SseEmitter.event()
					.name(NOTIFICATION_EVENT_NAME)
					.data(response));
				successCount++;
			} catch (IOException | IllegalStateException e) {
				log.warn("Failed to send notification SSE. connectionId={}", entry.getKey(), e);
				remove(receiverId, entry.getKey());
			}
		}

		return successCount;
	}

	@Scheduled(fixedDelayString = "${app.sse.heartbeat-interval-millis:30000}")
	public int sendHeartbeat() {
		int successCount = 0;

		for (var userEntry : emitters.entrySet()) {
			Long userId = userEntry.getKey();
			for (var emitterEntry : userEntry.getValue().entrySet()) {
				try {
					emitterEntry.getValue().send(SseEmitter.event()
						.comment(HEARTBEAT_COMMENT));
					successCount++;
				} catch (IOException | IllegalStateException e) {
					log.warn("Failed to send heartbeat SSE. connectionId={}", emitterEntry.getKey(), e);
					remove(userId, emitterEntry.getKey());
				}
			}
		}

		if (successCount > 0) {
			log.trace("SSE heartbeat sent. successCount={}, totalConnectionCount={}",
				successCount,
				getTotalConnectionCount()
			);
		}

		return successCount;
	}

	SseEmitter createEmitter() {
		return new SseEmitter(SSE_TIMEOUT_MILLIS);
	}

	private String register(Long userId, SseEmitter emitter) {
		String connectionId = UUID.randomUUID().toString();
		emitters.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>()).put(connectionId, emitter);

		emitter.onCompletion(() -> remove(userId, connectionId));
		emitter.onTimeout(() -> {
			log.info("SSE connection timed out. connectionId={}", connectionId);
			remove(userId, connectionId);
		});
		emitter.onError(ignored -> remove(userId, connectionId));

		log.info(
			"SSE connection registered. connectionId={}, userConnectionCount={}, totalConnectionCount={}",
			connectionId,
			getConnectionCount(userId),
			getTotalConnectionCount()
		);
		return connectionId;
	}

	private void sendConnectedEvent(Long userId, String connectionId, SseEmitter emitter) {
		try {
			emitter.send(SseEmitter.event()
				.name(CONNECTED_EVENT_NAME)
				.data(CONNECTED_EVENT_DATA));
		} catch (IOException | IllegalStateException e) {
			log.warn("Failed to send connected SSE. connectionId={}", connectionId, e);
			remove(userId, connectionId);
		}
	}

	void remove(Long userId, SseEmitter emitter) {
		ConcurrentHashMap<String, SseEmitter> userEmitters = emitters.get(userId);
		if (userEmitters == null) {
			return;
		}

		userEmitters.entrySet().removeIf(entry -> entry.getValue() == emitter);
		if (userEmitters.isEmpty()) {
			emitters.remove(userId, userEmitters);
		}
	}

	private void remove(Long userId, String connectionId) {
		ConcurrentHashMap<String, SseEmitter> userEmitters = emitters.get(userId);
		if (userEmitters == null) {
			return;
		}

		SseEmitter removed = userEmitters.remove(connectionId);
		if (userEmitters.isEmpty()) {
			emitters.remove(userId, userEmitters);
		}

		if (removed != null) {
			log.debug(
				"SSE connection removed. connectionId={}, userConnectionCount={}, totalConnectionCount={}",
				connectionId,
				getConnectionCount(userId),
				getTotalConnectionCount()
			);
		}
	}

	int getConnectionCount(Long userId) {
		ConcurrentHashMap<String, SseEmitter> userEmitters = emitters.get(userId);
		if (userEmitters == null) {
			return 0;
		}

		return userEmitters.size();
	}

	int getTotalConnectionCount() {
		return emitters.values().stream()
			.mapToInt(ConcurrentHashMap::size)
			.sum();
	}

	void clear() {
		emitters.clear();
	}
}
