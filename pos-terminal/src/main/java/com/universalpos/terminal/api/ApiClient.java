package com.universalpos.terminal.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.universalpos.terminal.model.SessionState;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Central HTTP client for all backend API communication.
 *
 * Why one class? So there's one place that:
 *   1. Sets the Authorization header (so you never forget it)
 *   2. Parses the response JSON
 *   3. Handles errors consistently
 *
 * Every call returns a JsonNode so callers can navigate the response
 * structure however they need. We unwrap the ApiResponse<T> wrapper
 * (the backend always returns {"success":true,"data":{...}}) here,
 * so service classes get the actual data directly.
 *
 * OkHttp is used instead of Java's built-in HttpClient because it has
 * better connection pooling, cleaner timeouts, and is used widely in
 * Android/desktop Java apps.
 */
public class ApiClient {

    private static ApiClient instance;

    private final OkHttpClient http;
    private final ObjectMapper json;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private ApiClient() {
        this.http = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        this.json = new ObjectMapper();
        this.json.registerModule(new JavaTimeModule());
        this.json.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static ApiClient getInstance() {
        if (instance == null) instance = new ApiClient();
        return instance;
    }

    // ── Core HTTP methods ─────────────────────────────────────

    /**
     * GET request — returns the "data" field from the ApiResponse wrapper.
     * Throws ApiException if the server returns success=false.
     */
    public JsonNode get(String path) throws ApiException {
        Request request = new Request.Builder()
                .url(url(path))
                .addHeader("Authorization", SessionState.getInstance().authHeader())
                .get()
                .build();
        return execute(request);
    }

    /**
     * POST request with a JSON body.
     */
    public JsonNode post(String path, Object body) throws ApiException {
        try {
            RequestBody requestBody = RequestBody.create(
                    json.writeValueAsString(body), JSON);
            Request request = new Request.Builder()
                    .url(url(path))
                    .addHeader("Authorization", SessionState.getInstance().authHeader())
                    .post(requestBody)
                    .build();
            return execute(request);
        } catch (Exception e) {
            throw new ApiException("Failed to serialize request body: " + e.getMessage());
        }
    }

    /**
     * POST without Authorization header — used only for /auth/login
     * since you don't have a token yet when you're logging in.
     */
    public JsonNode postPublic(String path, Object body) throws ApiException {
        try {
            RequestBody requestBody = RequestBody.create(
                    json.writeValueAsString(body), JSON);
            Request request = new Request.Builder()
                    .url(url(path))
                    .post(requestBody)
                    .build();
            return execute(request);
        } catch (Exception e) {
            throw new ApiException("Failed to serialize request body: " + e.getMessage());
        }
    }

    // ── Internal helpers ──────────────────────────────────────

    private JsonNode execute(Request request) throws ApiException {
        try (Response response = http.newCall(request).execute()) {
            String bodyStr = response.body() != null
                    ? response.body().string() : "{}";

            JsonNode root = json.readTree(bodyStr);

            // Backend always returns {"success":true/false,"message":"...","data":{...}}
            boolean success = root.path("success").asBoolean(false);

            if (!success) {
                String message = root.path("message").asText("Unknown error");
                throw new ApiException(message);
            }

            // Return just the data payload — callers don't need the wrapper
            return root.path("data");

        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new ApiException("Network error: " + e.getMessage());
        } catch (Exception e) {
            throw new ApiException("Unexpected error: " + e.getMessage());
        }
    }

    private String url(String path) {
        String base = SessionState.getInstance().getBaseUrl();
        return base + (path.startsWith("/") ? path : "/" + path);
    }

    public ObjectMapper getJson() { return json; }

    /**
     * Checked exception thrown when an API call fails.
     * Includes the server's error message so we can show it to the user.
     */
    public static class ApiException extends Exception {
        public ApiException(String message) { super(message); }
    }
}
