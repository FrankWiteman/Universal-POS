package com.universalpos.engine;

import com.universalpos.domain.Customer;
import com.universalpos.domain.Customer.LoyaltyTier;
import com.universalpos.domain.Discount;
import com.universalpos.domain.Product;
import com.universalpos.repository.DiscountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Discount Engine.
 *
 * No DB, no Spring context — pure logic testing with Mockito stubs.
 * This is the right way to test a calculation engine.
 */
@ExtendWith(MockitoExtension.class)
class DiscountEngineTest {

    @Mock
    DiscountRepository discountRepository;

    @InjectMocks
    DiscountEngine discountEngine;

    private Product dummyProduct;
    private Map<Product, Integer> cart;

    @BeforeEach
    void setUp() {
        // Inject the @Value field that Spring normally handles
        ReflectionTestUtils.setField(discountEngine, "maxDiscountPercent", 50.0);

        dummyProduct = Product.builder()
                .productId(1L)
                .name("Test Guitar")
                .price(new BigDecimal("500.00"))
                .taxable(true)
                .active(true)
                .build();

        cart = Map.of(dummyProduct, 1);
    }

    // ── PERCENT discounts ─────────────────────────────────────────

    @Test
    @DisplayName("Should apply 10% percent discount to subtotal")
    void shouldApplyPercentDiscount() {
        Discount tenPct = buildDiscount(Discount.DiscountType.PERCENT,
                new BigDecimal("10.00"), null, null);
        when(discountRepository.findValidDiscounts(anyLong(), any()))
                .thenReturn(List.of(tenPct));

        DiscountEngine.DiscountResult result = discountEngine.evaluate(
                1L, null, cart, new BigDecimal("500.00"), null);

        assertThat(result.hasDiscount()).isTrue();
        assertThat(result.discountAmount()).isEqualByComparingTo("50.00");
        assertThat(result.discountLabel()).isEqualTo("Test Discount");
    }

    @Test
    @DisplayName("Should NOT apply percent discount when minimum purchase not met")
    void shouldNotApplyWhenMinPurchaseNotMet() {
        Discount discount = buildDiscount(Discount.DiscountType.PERCENT,
                new BigDecimal("10.00"), new BigDecimal("1000.00"), null);
        when(discountRepository.findValidDiscounts(anyLong(), any()))
                .thenReturn(List.of(discount));

        DiscountEngine.DiscountResult result = discountEngine.evaluate(
                1L, null, cart, new BigDecimal("500.00"), null);

        assertThat(result.hasDiscount()).isFalse();
        assertThat(result.discountAmount()).isEqualByComparingTo("0.00");
    }

    // ── FIXED AMOUNT discounts ────────────────────────────────────

    @Test
    @DisplayName("Should apply $50 fixed discount on $500 cart")
    void shouldApplyFixedDiscount() {
        Discount fixed = buildDiscount(Discount.DiscountType.FIXED_AMOUNT,
                new BigDecimal("50.00"), new BigDecimal("100.00"), null);
        when(discountRepository.findValidDiscounts(anyLong(), any()))
                .thenReturn(List.of(fixed));

        DiscountEngine.DiscountResult result = discountEngine.evaluate(
                1L, null, cart, new BigDecimal("500.00"), null);

        assertThat(result.discountAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Fixed discount should never exceed subtotal")
    void fixedDiscountShouldNotExceedSubtotal() {
        Discount fixed = buildDiscount(Discount.DiscountType.FIXED_AMOUNT,
                new BigDecimal("999.00"), null, null);
        when(discountRepository.findValidDiscounts(anyLong(), any()))
                .thenReturn(List.of(fixed));

        DiscountEngine.DiscountResult result = discountEngine.evaluate(
                1L, null, cart, new BigDecimal("500.00"), null);

        // Should be capped at subtotal amount, not $999
        assertThat(result.discountAmount()).isEqualByComparingTo("500.00");
    }

    // ── LOYALTY TIER discounts ────────────────────────────────────

    @Test
    @DisplayName("Gold customer should receive Gold loyalty discount")
    void goldCustomerShouldReceiveLoyaltyDiscount() {
        Customer goldCustomer = Customer.builder()
                .customerId(1L)
                .firstName("Jane")
                .lastName("Doe")
                .loyaltyTier(LoyaltyTier.GOLD)
                .loyaltyPoints(5000)
                .build();

        Discount loyaltyDiscount = buildDiscount(Discount.DiscountType.LOYALTY_TIER,
                new BigDecimal("10.00"), null, LoyaltyTier.GOLD);
        when(discountRepository.findValidDiscounts(anyLong(), any()))
                .thenReturn(List.of(loyaltyDiscount));

        DiscountEngine.DiscountResult result = discountEngine.evaluate(
                1L, goldCustomer, cart, new BigDecimal("500.00"), null);

        assertThat(result.hasDiscount()).isTrue();
        assertThat(result.discountAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Bronze customer should NOT receive Gold loyalty discount")
    void bronzeCustomerShouldNotReceiveGoldDiscount() {
        Customer bronzeCustomer = Customer.builder()
                .customerId(2L)
                .firstName("Bob")
                .lastName("Smith")
                .loyaltyTier(LoyaltyTier.BRONZE)
                .loyaltyPoints(600)
                .build();

        Discount goldDiscount = buildDiscount(Discount.DiscountType.LOYALTY_TIER,
                new BigDecimal("15.00"), null, LoyaltyTier.GOLD);
        when(discountRepository.findValidDiscounts(anyLong(), any()))
                .thenReturn(List.of(goldDiscount));

        DiscountEngine.DiscountResult result = discountEngine.evaluate(
                1L, bronzeCustomer, cart, new BigDecimal("500.00"), null);

        assertThat(result.hasDiscount()).isFalse();
    }

    @Test
    @DisplayName("Walk-in customer (null) should not receive loyalty discount")
    void walkInCustomerShouldNotReceiveLoyaltyDiscount() {
        Discount loyaltyDiscount = buildDiscount(Discount.DiscountType.LOYALTY_TIER,
                new BigDecimal("10.00"), null, LoyaltyTier.BRONZE);
        when(discountRepository.findValidDiscounts(anyLong(), any()))
                .thenReturn(List.of(loyaltyDiscount));

        DiscountEngine.DiscountResult result = discountEngine.evaluate(
                1L, null, cart, new BigDecimal("500.00"), null);

        assertThat(result.hasDiscount()).isFalse();
    }

    // ── COUPON CODE discounts ─────────────────────────────────────

    @Test
    @DisplayName("Valid coupon code should apply discount")
    void validCouponShouldApply() {
        Discount coupon = Discount.builder()
                .discountId(10L)
                .name("Summer20")
                .discountType(Discount.DiscountType.COUPON_CODE)
                .value(new BigDecimal("20.00"))
                .couponCode("SUMMER20")
                .active(true)
                .build();
        when(discountRepository.findValidDiscounts(anyLong(), any()))
                .thenReturn(List.of(coupon));

        DiscountEngine.DiscountResult result = discountEngine.evaluate(
                1L, null, cart, new BigDecimal("500.00"), "SUMMER20");

        assertThat(result.hasDiscount()).isTrue();
    }

    @Test
    @DisplayName("Wrong coupon code should not apply")
    void wrongCouponCodeShouldNotApply() {
        Discount coupon = Discount.builder()
                .discountId(10L)
                .name("Summer20")
                .discountType(Discount.DiscountType.COUPON_CODE)
                .value(new BigDecimal("20.00"))
                .couponCode("SUMMER20")
                .active(true)
                .build();
        when(discountRepository.findValidDiscounts(anyLong(), any()))
                .thenReturn(List.of(coupon));

        DiscountEngine.DiscountResult result = discountEngine.evaluate(
                1L, null, cart, new BigDecimal("500.00"), "WRONGCODE");

        assertThat(result.hasDiscount()).isFalse();
    }

    // ── Best discount selection ───────────────────────────────────

    @Test
    @DisplayName("Engine should select the highest-value eligible discount")
    void shouldSelectBestDiscount() {
        Discount small = buildDiscount(Discount.DiscountType.PERCENT,
                new BigDecimal("5.00"), null, null);
        small = Discount.builder().discountId(1L).name("5% Off")
                .discountType(Discount.DiscountType.PERCENT)
                .value(new BigDecimal("5.00")).active(true).build();

        Discount large = Discount.builder().discountId(2L).name("20% Off")
                .discountType(Discount.DiscountType.PERCENT)
                .value(new BigDecimal("20.00")).active(true).build();

        when(discountRepository.findValidDiscounts(anyLong(), any()))
                .thenReturn(List.of(small, large));

        DiscountEngine.DiscountResult result = discountEngine.evaluate(
                1L, null, cart, new BigDecimal("500.00"), null);

        // 20% of 500 = $100, which beats 5% = $25
        assertThat(result.discountAmount()).isEqualByComparingTo("100.00");
        assertThat(result.discountLabel()).isEqualTo("20% Off");
    }

    @Test
    @DisplayName("Discount should be capped at maxDiscountPercent (50%) of subtotal")
    void shouldCapDiscountAtMaxPercent() {
        // 80% discount — should be capped at 50%
        Discount huge = Discount.builder().discountId(99L).name("80% Off")
                .discountType(Discount.DiscountType.PERCENT)
                .value(new BigDecimal("80.00")).active(true).build();

        when(discountRepository.findValidDiscounts(anyLong(), any()))
                .thenReturn(List.of(huge));

        DiscountEngine.DiscountResult result = discountEngine.evaluate(
                1L, null, cart, new BigDecimal("500.00"), null);

        // Should be capped at 50% of $500 = $250
        assertThat(result.discountAmount()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("No eligible discounts returns zero discount result")
    void noEligibleDiscountsShouldReturnNone() {
        when(discountRepository.findValidDiscounts(anyLong(), any()))
                .thenReturn(List.of());

        DiscountEngine.DiscountResult result = discountEngine.evaluate(
                1L, null, cart, new BigDecimal("500.00"), null);

        assertThat(result.hasDiscount()).isFalse();
        assertThat(result.discountAmount()).isEqualByComparingTo("0.00");
        assertThat(result.discountLabel()).isNull();
    }

    // ── Helper ───────────────────────────────────────────────────

    private Discount buildDiscount(Discount.DiscountType type, BigDecimal value,
                                    BigDecimal minPurchase, LoyaltyTier tierRequired) {
        return Discount.builder()
                .discountId(1L)
                .name("Test Discount")
                .discountType(type)
                .value(value)
                .minPurchase(minPurchase)
                .loyaltyTierRequired(tierRequired)
                .active(true)
                .build();
    }
}
