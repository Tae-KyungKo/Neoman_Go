package com.neomango.notification.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neomango.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findByReceiverId(Long receiverId, Pageable pageable);

	long countByReceiverIdAndReadAtIsNull(Long receiverId);

	Optional<Notification> findByIdAndReceiverId(Long notificationId, Long receiverId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update Notification notification
		set notification.readAt = CURRENT_TIMESTAMP
		where notification.receiver.id = :receiverId
			and notification.readAt is null
		""")
	int markAllAsReadByReceiverId(@Param("receiverId") Long receiverId);
}
