package com.neomango.notice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neomango.notice.dto.NoticeResponse;
import com.neomango.notice.dto.NoticeSummaryResponse;
import com.neomango.notice.entity.Notice;
import com.neomango.notice.entity.NoticeStatus;
import com.neomango.notice.exception.NoticeNotFoundException;
import com.neomango.notice.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

	private final NoticeRepository noticeRepository;

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
}
