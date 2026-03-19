package com.universalpos.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The authenticated "user" context attached to every secured request.
 *
 * Controllers extract this from the SecurityContext to know:
 *   - WHO is making the request (employeeId)
 *   - FOR WHICH COMPANY (tenantId) — enforces data isolation
 *   - WHAT THEY CAN DO (role)
 *
 * Usage in a controller:
 *   PosUserPrincipal principal = (PosUserPrincipal) SecurityContextHolder
 *       .getContext().getAuthentication().getPrincipal();
 *
 * Or via the @CurrentUser annotation (see SecurityConfig).
 */
@Getter
@AllArgsConstructor
public class PosUserPrincipal {

    private final Long   employeeId;
    private final Long   tenantId;
    private final String email;
    private final String role;

    public boolean isManager() {
        return "MANAGER".equals(role) || "ADMIN".equals(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
