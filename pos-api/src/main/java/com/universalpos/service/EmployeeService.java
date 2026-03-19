package com.universalpos.service;

import com.universalpos.domain.Employee;
import com.universalpos.domain.Employee.Role;
import com.universalpos.domain.Tenant;
import com.universalpos.exception.BusinessException;
import com.universalpos.exception.ResourceNotFoundException;
import com.universalpos.repository.EmployeeRepository;
import com.universalpos.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final TenantRepository   tenantRepository;
    private final PasswordEncoder    passwordEncoder;
    private final AuditService       auditService;

    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees(Long tenantId) {
        return employeeRepository.findAll().stream()
                .filter(e -> e.getTenant().getTenantId().equals(tenantId))
                .filter(Employee::getActive)
                .toList();
    }

    @Transactional(readOnly = true)
    public Employee getById(Long employeeId, Long tenantId) {
        return employeeRepository.findByEmployeeIdAndTenant_TenantId(employeeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
    }

    @Transactional
    public Employee createEmployee(String firstName, String lastName, String email,
                                    String password, Role role, Long tenantId,
                                    Long requestingEmployeeId, String requestingEmployeeName) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        // Check email unique within tenant
        employeeRepository.findActiveByEmailAndTenantId(email, tenantId).ifPresent(e -> {
            throw new BusinessException("An employee with email " + email + " already exists.");
        });

        Employee employee = Employee.builder()
                .tenant(tenant)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .active(true)
                .build();

        Employee saved = employeeRepository.save(employee);

        auditService.log(tenantId, requestingEmployeeId, requestingEmployeeName,
                "CREATE_EMPLOYEE", "EMPLOYEE", saved.getEmployeeId(),
                "New employee: " + saved.getFullName() + " (" + role + ")");

        log.info("Employee created: {} ({}) in tenant {}", saved.getFullName(), role, tenantId);
        return saved;
    }

    @Transactional
    public Employee updateEmployee(Long employeeId, String firstName, String lastName,
                                    Role role, Boolean active, Long tenantId,
                                    Long requestingEmployeeId, String requestingEmployeeName) {
        Employee employee = getById(employeeId, tenantId);

        if (firstName != null) employee.setFirstName(firstName);
        if (lastName  != null) employee.setLastName(lastName);
        if (role      != null) employee.setRole(role);
        if (active    != null) employee.setActive(active);

        Employee saved = employeeRepository.save(employee);

        auditService.log(tenantId, requestingEmployeeId, requestingEmployeeName,
                "UPDATE_EMPLOYEE", "EMPLOYEE", employeeId, "Employee updated");
        return saved;
    }

    @Transactional
    public void changePassword(Long employeeId, String newPassword, Long tenantId,
                                Long requestingEmployeeId, String requestingEmployeeName) {
        Employee employee = getById(employeeId, tenantId);
        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);

        auditService.log(tenantId, requestingEmployeeId, requestingEmployeeName,
                "CHANGE_PASSWORD", "EMPLOYEE", employeeId, "Password changed");
        log.info("Password changed for employee {}", employeeId);
    }
}
