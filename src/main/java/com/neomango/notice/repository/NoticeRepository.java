package com.neomango.notice.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.neomango.notice.entity.Notice;
import com.neomango.notice.entity.NoticeStatus;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

	Page<Notice> findByStatus(NoticeStatus status, Pageable pageable);

	Optional<Notice> findByIdAndStatus(Long id, NoticeStatus status);
}
