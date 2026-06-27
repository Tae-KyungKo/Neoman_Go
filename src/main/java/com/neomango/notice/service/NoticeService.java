package com.neomango.notice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neomango.audit.entity.AuditLog;
import com.neomango.audit.repository.AuditLogRepository;
import com.neomango.global.exception.BusinessException;
import com.neomango.global.exception.ErrorCode;
import com.neomango.notice.dto.NoticeCreateRequest;
import com.neomango.notice.dto.NoticeResponse;
import com.neomango.notice.dto.NoticeSummaryResponse;
import com.neomango.notice.dto.NoticeUpdateRequest;
import com.neomango.notice.entity.Notice;
import com.neomango.notice.entity.NoticeStatus;
import com.neomango.notice.exception.NoticeNotFoundException;
import com.neomango.notice.repository.NoticeRepository;
import com.neomango.user.entity.User;
import com.neomango.user.entity.UserStatus;
import com.neomango.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

	private final NoticeRepository noticeRepository;
	private final UserRepository userRepository;
	private final AuditLogRepository auditLogRepository;

	public Page<NoticeSummaryResponse> getNotices(Pageable pageable) {
		Pageable latestPageable = PageRequest.of(
			pageable.getPageNumber(),
			pageable.getPageSize(),
			Sort.by(Sort.Direction.DESC, "createdAt", "id")
		);

		return noticeRepository.findByStatus(NoticeStatus.ACTIVE, latestPageable)
			.map(NoticeSummaryResponse::from);
	}

	public NoticeResponse getNotice(Long noticeId) {
		Notice notice = noticeRepository.findByIdAndStatus(noticeId, NoticeStatus.ACTIVE)
			.orElseThrow(NoticeNotFoundException::new);

		return NoticeResponse.from(notice);
	}

	@Transactional
	public NoticeResponse createNotice(Long adminId, NoticeCreateRequest request) {
		User admin = userRepository.findById(adminId)
			.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		validateActiveUser(admin);

		Notice notice = Notice.create(admin, request.title(), request.content());
		Notice savedNotice = noticeRepository.save(notice);
		auditLogRepository.save(AuditLog.noticeCreated(adminId, savedNotice.getId()));

		return NoticeResponse.from(savedNotice);
	}

	@Transactional
	public NoticeResponse updateNotice(Long adminId, Long noticeId, NoticeUpdateRequest request) {
		Notice notice = noticeRepository.findByIdAndStatus(noticeId, NoticeStatus.ACTIVE)
			.orElseThrow(NoticeNotFoundException::new);

		notice.update(request.title(), request.content());
		auditLogRepository.save(AuditLog.noticeUpdated(adminId, notice.getId()));

		return NoticeResponse.from(notice);
	}

	@Transactional
	public void deleteNotice(Long adminId, Long noticeId) {
		Notice notice = noticeRepository.findByIdAndStatus(noticeId, NoticeStatus.ACTIVE)
			.orElseThrow(NoticeNotFoundException::new);

		notice.softDelete();
		auditLogRepository.save(AuditLog.noticeDeleted(adminId, notice.getId()));
	}

	private void validateActiveUser(User user) {
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
	}
}
