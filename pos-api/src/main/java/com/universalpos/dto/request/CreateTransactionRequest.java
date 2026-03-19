package com.universalpos.dto.request;

import com.universalpos.domain.Transaction.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateTransactionRequest {

    /** Optional — null means walk-in customer, no loyalty */
    private Long   customerId;

    /** Optional coupon code entered at checkout */
    private String couponCode;

    @NotEmpty(message = "Transaction must have at least one item")
    @Valid
    private List<TransactionItemRequest> items;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    /** For cash transactions — how much the customer handed over */
    private BigDecimal amountTendered;

    private String notes;

    @Data
    public static class TransactionItemRequest {
        @NotNull private Long    productId;
        @NotNull private Integer qty;
        /** Optional manual price override (requires MANAGER role) */
        private BigDecimal manualPriceOverride;
    }
}
