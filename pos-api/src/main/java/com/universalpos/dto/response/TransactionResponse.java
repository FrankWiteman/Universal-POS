package com.universalpos.dto.response;

import com.universalpos.domain.Transaction.PaymentMethod;
import com.universalpos.domain.Transaction.TransactionStatus;
import com.universalpos.domain.Transaction.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TransactionResponse {

    private Long              txnId;
    private String            receiptNumber;
    private CustomerResponse  customer;
    private String            employeeName;
    private List<LineItemResponse> items;
    private BigDecimal        subtotal;
    private BigDecimal        discountAmount;
    private BigDecimal        taxAmount;
    private BigDecimal        total;
    private BigDecimal        amountTendered;
    private BigDecimal        changeDue;
    private PaymentMethod     paymentMethod;
    private TransactionStatus status;
    private TransactionType   txnType;
    private Integer           loyaltyPointsEarned;
    private LocalDateTime     completedAt;

    @Data
    @Builder
    public static class LineItemResponse {
        private Long       productId;
        private String     productName;
        private String     sku;
        private Integer    qty;
        private BigDecimal unitPrice;
        private BigDecimal discountApplied;
        private String     discountLabel;
        private BigDecimal lineTotal;
    }
}
