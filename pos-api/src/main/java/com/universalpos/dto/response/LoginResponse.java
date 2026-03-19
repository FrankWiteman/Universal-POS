package com.universalpos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Returned to the POS terminal after successful authentication.
 */
@Data
@Builder
@AllArgsConstructor
public class LoginResponse {

    private String  token;
    private Long    employeeId;
    private String  employeeName;
    private String  role;
    private Long    tenantId;
    private String  tenantSlug;
    private String  companyName;
    private long    expiresInMs;
}
