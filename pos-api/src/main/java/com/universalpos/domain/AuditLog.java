package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable audit trail entry.
 * Every mutating action (transaction, void, discount, employee change) is recorded here.
 *
 * This is append-only — records are never updated or deleted.
 */
@Entity
@Table(name = "AUDIT_LOG")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_seq")
    @SequenceGenerator(name = "audit_seq", sequenceName = "AUDIT_SEQ", allocationSize = 1)
    @Column(name = "LOG_ID")
    private Long logId;

    @Column(name = "TENANT_ID", nullable = false)
    private Long tenantId;

    @Column(name = "EMPLOYEE_ID")
    private Long employeeId;

    @Column(name = "EMPLOYEE_NAME", length = 120)
    private String employeeName;

    @Column(name = "ACTION", nullable = false, length = 50)
    private String action;

    @Column(name = "ENTITY_TYPE", length = 50)
    private String entityType;

    @Column(name = "ENTITY_ID")
    private Long entityId;

    /** JSON blob with before/after state or relevant context */
    @Column(name = "DETAILS", length = 2000)
    private String details;

    @Column(name = "IP_ADDRESS", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
