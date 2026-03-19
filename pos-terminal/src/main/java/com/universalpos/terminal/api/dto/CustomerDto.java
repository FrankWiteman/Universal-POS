package com.universalpos.terminal.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Subset of the backend Customer entity we need in the terminal */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerDto {
    private Long   customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String loyaltyTier;
    private Integer loyaltyPoints;
    private String loyaltyCardNumber;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getDisplayInfo() {
        return getFullName() + " | " + loyaltyTier + " | " + loyaltyPoints + " pts";
    }
}
