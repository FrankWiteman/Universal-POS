package com.universalpos.service;

import com.universalpos.domain.AuditLog;
import com.universalpos.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes audit log entries.
 *
 * Uses REQUIRES_NEW propagation so audit logs are committed
 * even if the parent transaction rolls back.
 *
 * Annotated @Async so logging never slows down the POS response.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long tenantId, Long employeeId, String employeeName,
                    String action, String entityType, Long entityId, String details) {
        try {
            AuditLog entry = AuditLog.builder()
                    .tenantId(tenantId)
                    .employeeId(employeeId)
                    .employeeName(employeeName)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Audit failures must never break the main flow
            log.error("Failed to write audit log entry: action={} entity={} id={}",
                      action, entityType, entityId, e);
        }
    }
}
