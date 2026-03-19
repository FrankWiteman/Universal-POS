package com.universalpos.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * An employee who can operate the POS terminal.
 *
 * Roles:
 *   CASHIER  — ring up sales, process returns with limit, look up customers
 *   MANAGER  — everything a cashier can do + void transactions, apply manual discounts,
 *              manage products, view reports
 *   ADMIN    — full system access, manage employees, configure tenant settings
 */
@Entity
@Table(name = "EMPLOYEES",
       uniqueConstraints = {
           @UniqueConstraint(name = "UQ_EMPLOYEE_EMAIL_TENANT",
                            columnNames = {"EMAIL", "TENANT_ID"})
       })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_seq")
    @SequenceGenerator(name = "employee_seq", sequenceName = "EMPLOYEE_SEQ", allocationSize = 1)
    @Column(name = "EMPLOYEE_ID")
    private Long employeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TENANT_ID", nullable = false)
    private Tenant tenant;

    @Column(name = "FIRST_NAME", nullable = false, length = 60)
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false, length = 60)
    private String lastName;

    @Column(name = "EMAIL", nullable = false, length = 150)
    private String email;

    /** BCrypt-hashed password — never stored in plaintext */
    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    private Role role;

    /** Employee number displayed on receipts */
    @Column(name = "EMPLOYEE_NUMBER", length = 20)
    private String employeeNumber;

    @Column(name = "ACTIVE", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    // ── Helpers ──────────────────────────────────────────────────

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean hasRole(Role requiredRole) {
        return this.role.ordinal() >= requiredRole.ordinal();
    }

    /**
     * Roles ordered by privilege level (CASHIER < MANAGER < ADMIN).
     * ordinal() comparison used in hasRole() above.
     */
    public enum Role {
        CASHIER,
        MANAGER,
        ADMIN
    }
}
