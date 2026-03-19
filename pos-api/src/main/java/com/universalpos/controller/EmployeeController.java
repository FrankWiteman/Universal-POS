package com.universalpos.controller;

import com.universalpos.domain.Employee;
import com.universalpos.domain.Employee.Role;
import com.universalpos.dto.response.ApiResponse;
import com.universalpos.security.PosUserPrincipal;
import com.universalpos.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Staff management")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "List all active employees (Manager+ only)")
    public ResponseEntity<ApiResponse<List<Employee>>> list(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                employeeService.getAllEmployees(principal.getTenantId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get employee by ID (Manager+ only)")
    public ResponseEntity<ApiResponse<Employee>> getById(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                employeeService.getById(id, principal.getTenantId())));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new employee (Admin only)")
    public ResponseEntity<ApiResponse<Employee>> create(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @RequestBody Map<String, String> body) {

        Employee saved = employeeService.createEmployee(
                body.get("firstName"),
                body.get("lastName"),
                body.get("email"),
                body.get("password"),
                Role.valueOf(body.getOrDefault("role", "CASHIER")),
                principal.getTenantId(),
                principal.getEmployeeId(),
                principal.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Employee created", saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an employee (Admin only)")
    public ResponseEntity<ApiResponse<Employee>> update(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Role role = body.containsKey("role") ? Role.valueOf(body.get("role")) : null;
        Boolean active = body.containsKey("active") ? Boolean.valueOf(body.get("active")) : null;

        Employee saved = employeeService.updateEmployee(
                id,
                body.get("firstName"),
                body.get("lastName"),
                role, active,
                principal.getTenantId(),
                principal.getEmployeeId(),
                principal.getEmail());

        return ResponseEntity.ok(ApiResponse.ok("Employee updated", saved));
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change an employee's password (Admin only)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal @NonNull PosUserPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        employeeService.changePassword(
                id,
                body.get("newPassword"),
                principal.getTenantId(),
                principal.getEmployeeId(),
                principal.getEmail());

        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }
}
