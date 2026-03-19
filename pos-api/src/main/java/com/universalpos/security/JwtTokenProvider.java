package com.universalpos.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Handles JWT creation, parsing, and validation.
 *
 * Every POS terminal login receives a JWT that identifies:
 *   - which employee is logged in
 *   - which tenant they belong to
 *   - their role (CASHIER / MANAGER / ADMIN)
 *
 * This token is sent on every API request in the Authorization header.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a JWT for an authenticated employee.
     *
     * Claims included:
     *   sub         → employee email
     *   employeeId  → DB primary key
     *   tenantId    → tenant DB primary key
     *   tenantSlug  → e.g. "demo-store"
     *   role        → CASHIER / MANAGER / ADMIN
     */
    public String generateToken(Long employeeId, String email,
                                 Long tenantId, String tenantSlug, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("employeeId", employeeId)
                .claim("tenantId", tenantId)
                .claim("tenantSlug", tenantSlug)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getEmployeeIdFromToken(String token) {
        return parseClaims(token).get("employeeId", Long.class);
    }

    public Long getTenantIdFromToken(String token) {
        return parseClaims(token).get("tenantId", Long.class);
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
