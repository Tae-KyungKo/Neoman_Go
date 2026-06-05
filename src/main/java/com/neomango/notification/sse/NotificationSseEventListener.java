package com.neomango.notification.sse;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.neomango.notification.dto.NotificationResponse;
import com.neomango.notification.event.NotificationCreatedEvent;
import com.neomango.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSseEventListener {

	private final NotificationRepository notificationRepository;
	private final NotificationSseService notificationSseService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
	public void handle(NotificationCreatedEvent event) {
		try {
			notificationRepository.findByIdAndReceiverId(event.notificationId(), event.receiverId())
				.map(NotificationResponse::from)
				.ifPresent(response -> notificationSseService.sendToUser(event.receiverId(), response));
		} catch (RuntimeException e) {
			log.warn(
				"Failed to send notification SSE. notificationId={}, receiverId={}",
				event.notificationId(),
				event.receiverId(),
				e
			);
		}
	}
}
