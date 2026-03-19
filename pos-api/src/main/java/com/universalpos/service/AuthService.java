package com.universalpos.service;

import com.universalpos.domain.Employee;
import com.universalpos.dto.request.LoginRequest;
import com.universalpos.dto.response.LoginResponse;
import com.universalpos.exception.ResourceNotFoundException;
import com.universalpos.repository.EmployeeRepository;
import com.universalpos.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtTokenProvider   jwtTokenProvider;
    private final AuditService       auditService;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. Find active employee by email + tenant
        Employee employee = employeeRepository
                .findByEmailAndTenant_TenantSlugAndActiveTrue(
                        request.getEmail(), request.getTenantSlug())
                .orElseThrow(() -> {
                    log.warn("Login attempt failed — employee not found: {} @ {}",
                             request.getEmail(), request.getTenantSlug());
                    return new BadCredentialsException("Invalid email or password");
                });

        // 2. Validate password
        if (!passwordEncoder.matches(request.getPassword(), employee.getPasswordHash())) {
            log.warn("Login attempt failed — bad password for: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        // 3. Update last login timestamp
        employee.setLastLoginAt(LocalDateTime.now());
        employeeRepository.save(employee);

        // 4. Generate JWT
        String token = jwtTokenProvider.generateToken(
                employee.getEmployeeId(),
                employee.getEmail(),
                employee.getTenant().getTenantId(),
                employee.getTenant().getTenantSlug(),
                employee.getRole().name()
        );

        // 5. Audit the login event
        auditService.log(
                employee.getTenant().getTenantId(),
                employee.getEmployeeId(),
                employee.getFullName(),
                "LOGIN",
                "EMPLOYEE",
                employee.getEmployeeId(),
                "Successful login"
        );

        log.info("Employee {} ({}) logged in to tenant {}",
                 employee.getFullName(), employee.getRole(), employee.getTenant().getTenantSlug());

        return LoginResponse.builder()
                .token(token)
                .employeeId(employee.getEmployeeId())
                .employeeName(employee.getFullName())
                .role(employee.getRole().name())
                .tenantId(employee.getTenant().getTenantId())
                .tenantSlug(employee.getTenant().getTenantSlug())
                .companyName(employee.getTenant().getCompanyName())
                .expiresInMs(jwtExpirationMs)
                .build();
    }
}
