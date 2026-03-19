package com.universalpos.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateCustomerRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Must be a valid email address")
    private String email;

    @Pattern(regexp = "^[\\d\\s\\-\\+\\(\\)]{7,20}$", message = "Invalid phone number format")
    private String phone;

    private LocalDate dateOfBirth;
    private Boolean   emailOptIn = false;
    private String    notes;
}
