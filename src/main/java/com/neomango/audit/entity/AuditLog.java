package com.neomango.audit.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "audit_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long adminId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AuditResourceType resourceType;

	@Column(nullable = false)
	private Long resourceId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AuditAction action;

	@Column(nullable = false)
	private LocalDateTime performedAt;

	private AuditLog(Long adminId, AuditResourceType resourceType, Long resourceId, AuditAction action) {
		this.adminId = adminId;
		this.resourceType = resourceType;
		this.resourceId = resourceId;
		this.action = action;
		this.performedAt = LocalDateTime.now();
	}

	public static AuditLog noticeCreated(Long adminId, Long noticeId) {
		return new AuditLog(adminId, AuditResourceType.NOTICE, noticeId, AuditAction.CREATE);
	}

	public static AuditLog noticeUpdated(Long adminId, Long noticeId) {
		return new AuditLog(adminId, AuditResourceType.NOTICE, noticeId, AuditAction.UPDATE);
	}

	public static AuditLog noticeDeleted(Long adminId, Long noticeId) {
		return new AuditLog(adminId, AuditResourceType.NOTICE, noticeId, AuditAction.DELETE);
	}
}
