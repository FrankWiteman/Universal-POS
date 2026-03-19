package com.universalpos.terminal.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;

/** Summary of a completed transaction — shown on receipt prompt screen */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionResponse {
    private Long       txnId;
    private String     receiptNumber;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private BigDecimal amountTendered;
    private BigDecimal changeDue;
    private String     paymentMethod;
    private String     status;
    private String     txnType;
    private Integer    loyaltyPointsEarned;
}
