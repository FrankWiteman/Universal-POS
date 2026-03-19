package com.universalpos.dto.request;

import com.universalpos.domain.Transaction.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Request body for processing a return or exchange.
 *
 * For a RETURN: provide originalTxnId + items to return + refundPaymentMethod
 * For an EXCHANGE: provide all of the above + exchangeItems (new items being purchased)
 */
@Data
public class CreateReturnRequest {

    /** The original sale transaction being returned against */
    @NotNull(message = "Original transaction ID is required")
    private Long originalTxnId;

    @NotEmpty(message = "Must specify at least one item to return")
    @Valid
    private List<ReturnLineRequest> returnItems;

    /**
     * For EXCHANGE only — new items the customer is taking instead.
     * If null or empty, this is a plain RETURN.
     */
    private List<CreateTransactionRequest.TransactionItemRequest> exchangeItems;

    /** How the refund is issued — usually same as original payment method */
    @NotNull(message = "Refund payment method is required")
    private PaymentMethod refundPaymentMethod;

    private String notes;

    @Data
    public static class ReturnLineRequest {
        /** ID of the original TransactionItem being returned */
        @NotNull
        private Long originalItemId;

        /** How many units to return (must be <= qty on original item) */
        @NotNull
        private Integer qtyToReturn;

        /** Reason code ID from RETURN_REASONS table (optional but recommended) */
        private Long reasonId;

        /**
         * Should the item go back into stock?
         * Default true — set false if item is damaged/unsellable.
         */
        private Boolean restock = true;
    }
}
