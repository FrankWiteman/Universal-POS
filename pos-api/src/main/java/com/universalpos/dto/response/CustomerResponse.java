package com.universalpos.dto.response;

import com.universalpos.domain.Customer.LoyaltyTier;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponse {

    private Long         customerId;
    private String       firstName;
    private String       lastName;
    private String       fullName;
    private String       email;
    private String       phone;
    private String       loyaltyCardNumber;
    private LoyaltyTier  loyaltyTier;
    private Integer      loyaltyPoints;
    private Integer      pointsToNextTier;
    private LoyaltyTier  nextTier;
    private LocalDate    dateOfBirth;
    private Boolean      emailOptIn;
    private Boolean      vip;
    private LocalDateTime createdAt;
}
