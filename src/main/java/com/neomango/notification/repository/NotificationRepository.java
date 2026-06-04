package com.neomango.notification.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.neomango.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findByReceiverId(Long receiverId, Pageable pageable);

	long countByReceiverIdAndReadAtIsNull(Long receiverId);

	Optional<Notification> findByIdAndReceiverId(Long notificationId, Long receiverId);
}
