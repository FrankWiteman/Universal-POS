package com.universalpos.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Payload sent by the POS terminal on employee login.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * The tenant the employee belongs to.
     * This allows the same email to exist across multiple tenants.
     */
    @NotBlank(message = "Tenant slug is required")
    private String tenantSlug;
}
