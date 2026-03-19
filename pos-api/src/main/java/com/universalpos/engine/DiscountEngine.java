package com.universalpos.engine;

import com.universalpos.domain.Customer;
import com.universalpos.domain.Discount;
import com.universalpos.domain.Product;
import com.universalpos.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * ============================================================
 *  UniversalPOS Discount Engine
 * ============================================================
 *
 * Evaluates all applicable discount rules for a given cart
 * and customer, then selects and applies the best discount.
 *
 * Design principles:
 *   1. BEST DISCOUNT WINS — we find all eligible discounts and
 *      pick the one that gives the customer the most savings.
 *      No stacking (configurable per tenant in future phases).
 *
 *   2. RULE-BASED — no hardcoded "Guitar Center logic".
 *      Every rule is a row in the DISCOUNTS table, configurable
 *      by tenant managers without code changes.
 *
 *   3. PURE CALCULATION — engine does NOT mutate any DB state.
 *      It returns a DiscountResult; the TransactionService writes.
 *
 * Supported discount types:
 *   PERCENT        — X% off subtotal (e.g. Silver tier 5% off)
 *   FIXED_AMOUNT   — $X off when subtotal >= minimum (e.g. $10 off $100+)
 *   LOYALTY_TIER   — automatic tier-based percent discount
 *   COUPON_CODE    — code entered at checkout
 *   EMPLOYEE       — manager-applied discount
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DiscountEngine {

    private final DiscountRepository discountRepository;

    @Value("${universalpos.max-discount-percent:50.0}")
    private double maxDiscountPercent;

    // ── Public API ───────────────────────────────────────────────

    /**
     * Calculate the best discount for the given cart.
     *
     * @param tenantId    Which tenant's discount rules to evaluate
     * @param customer    The customer (may be null for walk-in)
     * @param cartItems   Map of Product → quantity
     * @param subtotal    Cart subtotal before any discounts
     * @param couponCode  Optional coupon code entered at checkout
     * @return            DiscountResult with amount to deduct and label
     */
    public DiscountResult evaluate(Long tenantId,
                                   Customer customer,
                                   Map<Product, Integer> cartItems,
                                   BigDecimal subtotal,
                                   String couponCode) {

        List<Discount> validDiscounts =
                discountRepository.findValidDiscounts(tenantId, LocalDate.now());

        log.debug("Evaluating {} valid discount rules for tenant {} | subtotal={}",
                  validDiscounts.size(), tenantId, subtotal);

        List<DiscountResult> eligibleResults = new ArrayList<>();

        for (Discount discount : validDiscounts) {
            Optional<DiscountResult> result =
                    evaluate(discount, customer, subtotal, couponCode);
            result.ifPresent(eligibleResults::add);
        }

        if (eligibleResults.isEmpty()) {
            log.debug("No eligible discounts found for this transaction.");
            return DiscountResult.none();
        }

        // Best discount = highest dollar savings
        DiscountResult best = eligibleResults.stream()
                .max(Comparator.comparing(DiscountResult::discountAmount))
                .orElse(DiscountResult.none());

        // Hard cap: never discount more than maxDiscountPercent of the subtotal
        BigDecimal cap = subtotal.multiply(
                BigDecimal.valueOf(maxDiscountPercent / 100.0))
                .setScale(2, RoundingMode.HALF_UP);

        if (best.discountAmount().compareTo(cap) > 0) {
            log.warn("Discount {} capped at {}% of subtotal (was {})",
                     best.discountLabel(), maxDiscountPercent, best.discountAmount());
            best = new DiscountResult(cap, best.discountLabel(), best.discountId());
        }

        log.info("Best discount selected: {} → saves ${}", best.discountLabel(), best.discountAmount());
        return best;
    }

    // ── Private evaluation logic ─────────────────────────────────

    private Optional<DiscountResult> evaluate(Discount discount,
                                               Customer customer,
                                               BigDecimal subtotal,
                                               String couponCode) {
        return switch (discount.getDiscountType()) {
            case PERCENT       -> evaluatePercent(discount, subtotal);
            case FIXED_AMOUNT  -> evaluateFixed(discount, subtotal);
            case LOYALTY_TIER  -> evaluateLoyaltyTier(discount, customer, subtotal);
            case COUPON_CODE   -> evaluateCoupon(discount, couponCode, subtotal);
            case EMPLOYEE      -> Optional.empty(); // Employee discounts applied via manager override
        };
    }

    private Optional<DiscountResult> evaluatePercent(Discount discount, BigDecimal subtotal) {
        if (!meetsMinPurchase(discount, subtotal)) return Optional.empty();

        BigDecimal amount = subtotal
                .multiply(discount.getValue().divide(BigDecimal.valueOf(100)))
                .setScale(2, RoundingMode.HALF_UP);

        return Optional.of(new DiscountResult(amount, discount.getName(), discount.getDiscountId()));
    }

    private Optional<DiscountResult> evaluateFixed(Discount discount, BigDecimal subtotal) {
        if (!meetsMinPurchase(discount, subtotal)) return Optional.empty();

        // Fixed discount can never exceed subtotal
        BigDecimal amount = discount.getValue().min(subtotal)
                .setScale(2, RoundingMode.HALF_UP);

        return Optional.of(new DiscountResult(amount, discount.getName(), discount.getDiscountId()));
    }

    private Optional<DiscountResult> evaluateLoyaltyTier(Discount discount,
                                                          Customer customer,
                                                          BigDecimal subtotal) {
        if (customer == null) return Optional.empty();
        if (discount.getLoyaltyTierRequired() == null) return Optional.empty();
        if (!meetsMinPurchase(discount, subtotal)) return Optional.empty();

        // Customer tier must be >= the required tier
        boolean eligible = customer.getLoyaltyTier().ordinal()
                >= discount.getLoyaltyTierRequired().ordinal();

        if (!eligible) return Optional.empty();

        BigDecimal amount = subtotal
                .multiply(discount.getValue().divide(BigDecimal.valueOf(100)))
                .setScale(2, RoundingMode.HALF_UP);

        return Optional.of(new DiscountResult(amount, discount.getName(), discount.getDiscountId()));
    }

    private Optional<DiscountResult> evaluateCoupon(Discount discount,
                                                     String couponCode,
                                                     BigDecimal subtotal) {
        if (couponCode == null || couponCode.isBlank()) return Optional.empty();
        if (!couponCode.equalsIgnoreCase(discount.getCouponCode())) return Optional.empty();
        if (!meetsMinPurchase(discount, subtotal)) return Optional.empty();

        return switch (discount.getDiscountType()) {
            case COUPON_CODE -> {
                // Coupon can be percent or fixed — check the value range
                boolean isPercent = discount.getValue().compareTo(BigDecimal.valueOf(100)) <= 0
                        && discount.getValue().compareTo(BigDecimal.ONE) >= 0;
                BigDecimal amount;
                if (isPercent) {
                    amount = subtotal.multiply(
                            discount.getValue().divide(BigDecimal.valueOf(100)))
                            .setScale(2, RoundingMode.HALF_UP);
                } else {
                    amount = discount.getValue().min(subtotal);
                }
                yield Optional.of(new DiscountResult(amount, discount.getName() + " (coupon)", discount.getDiscountId()));
            }
            default -> Optional.empty();
        };
    }

    private boolean meetsMinPurchase(Discount discount, BigDecimal subtotal) {
        if (discount.getMinPurchase() == null) return true;
        return subtotal.compareTo(discount.getMinPurchase()) >= 0;
    }

    // ── Result record ────────────────────────────────────────────

    /**
     * Immutable result from the discount engine.
     * Passed to TransactionService to apply to the transaction.
     */
    public record DiscountResult(
            BigDecimal discountAmount,
            String     discountLabel,
            Long       discountId
    ) {
        public static DiscountResult none() {
            return new DiscountResult(BigDecimal.ZERO, null, null);
        }

        public boolean hasDiscount() {
            return discountAmount.compareTo(BigDecimal.ZERO) > 0;
        }
    }
}
