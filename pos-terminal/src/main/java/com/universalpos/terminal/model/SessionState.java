package com.universalpos.terminal.model;

/**
 * Singleton holding the currently logged-in session.
 *
 * Why a singleton? Every screen in the terminal needs access to the JWT
 * token (to make API calls) and the employee info (to show who's logged in).
 * Rather than passing these values through every constructor, we store them
 * here and any class can call SessionState.getInstance().
 *
 * Why not Spring? pos-terminal has no Spring context — it's a plain JavaFX
 * desktop app. We manage state manually.
 */
public class SessionState {

    // The one and only instance
    private static SessionState instance;

    private String jwtToken;
    private Long   employeeId;
    private String employeeName;
    private String role;            // CASHIER, MANAGER, or ADMIN
    private String tenantSlug;
    private String baseUrl = "http://localhost:8080/api";

    // Private constructor — nobody can call new SessionState() from outside
    private SessionState() {}

    /** Get (or create) the single instance */
    public static SessionState getInstance() {
        if (instance == null) {
            instance = new SessionState();
        }
        return instance;
    }

    /** Called after a successful login */
    public void login(String token, Long employeeId,
                      String employeeName, String role, String tenantSlug) {
        this.jwtToken     = token;
        this.employeeId   = employeeId;
        this.employeeName = employeeName;
        this.role         = role;
        this.tenantSlug   = tenantSlug;
    }

    /** Called on logout — clears everything */
    public void logout() {
        this.jwtToken     = null;
        this.employeeId   = null;
        this.employeeName = null;
        this.role         = null;
    }

    public boolean isLoggedIn()  { return jwtToken != null; }
    public boolean isManager()   { return "MANAGER".equals(role) || "ADMIN".equals(role); }
    public boolean isAdmin()     { return "ADMIN".equals(role); }

    public String getJwtToken()      { return jwtToken; }
    public Long   getEmployeeId()    { return employeeId; }
    public String getEmployeeName()  { return employeeName; }
    public String getRole()          { return role; }
    public String getTenantSlug()    { return tenantSlug; }
    public String getBaseUrl()       { return baseUrl; }

    public void setBaseUrl(String url) { this.baseUrl = url; }

    /** Returns the Authorization header value — used by every API call */
    public String authHeader() {
        return "Bearer " + jwtToken;
    }
}
