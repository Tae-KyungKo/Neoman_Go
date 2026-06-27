package com.neomango.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neomango.audit.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
