package com.universalpos.terminal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.universalpos.terminal.api.ApiClient;
import com.universalpos.terminal.api.ApiClient.ApiException;
import com.universalpos.terminal.api.dto.LoginResponse;
import com.universalpos.terminal.model.SessionState;

import java.util.Map;

/**
 * Handles authentication against the backend API.
 * On success, populates SessionState so the rest of the app has the token.
 */
public class AuthService {

    private final ApiClient api = ApiClient.getInstance();

    /**
     * Log in with email, password, and tenant slug.
     * Returns the employee name on success, throws ApiException on failure.
     */
    public String login(String email, String password, String tenantSlug)
            throws ApiException {

        // Build the request body — matches backend LoginRequest DTO
        Map<String, String> body = Map.of(
                "email",      email,
                "password",   password,
                "tenantSlug", tenantSlug
        );

        JsonNode data = api.postPublic("/auth/login", body);

        // Deserialize the response data into our LoginResponse DTO
        LoginResponse response = api.getJson()
                .convertValue(data, LoginResponse.class);

        // Store everything in the session singleton
        SessionState.getInstance().login(
                response.getToken(),
                response.getEmployeeId(),
                response.getEmployeeName(),
                response.getRole(),
                response.getTenantSlug()
        );

        return response.getEmployeeName();
    }

    public void logout() {
        SessionState.getInstance().logout();
    }
}
