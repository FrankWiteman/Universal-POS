package com.universalpos.terminal.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Mirrors the backend LoginResponse DTO */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {
    private String token;
    private Long   employeeId;
    private String employeeName;
    private String email;
    private String role;
    private String tenantSlug;
}
