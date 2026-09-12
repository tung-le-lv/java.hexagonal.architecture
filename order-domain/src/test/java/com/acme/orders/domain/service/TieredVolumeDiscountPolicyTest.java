package com.acme.orders.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.orders.domain.aggregate.Order;
import com.acme.orders.domain.valueobject.Address;
import com.acme.orders.domain.valueobject.CustomerId;
import com.acme.orders.domain.valueobject.Money;
import com.acme.orders.domain.valueobject.ProductId;
import com.acme.orders.domain.valueobject.Quantity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TieredVolumeDiscountPolicyTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    private final IDiscountPolicy policy = new TieredVolumeDiscountPolicy(List.of(
            new TieredVolumeDiscountPolicy.Tier(Money.of("200.00", "EUR"), BigDecimal.valueOf(5)),
            new TieredVolumeDiscountPolicy.Tier(Money.of("500.00", "EUR"), BigDecimal.valueOf(10))));

    @Test
    @DisplayName("a basket below every threshold gets nothing off")
    void belowAllTiers() {
        assertThat(policy.discountFor(orderWorth("199.99"))).isEqualTo(Money.zero(EUR));
    }

    @Test
    @DisplayName("the highest tier the basket reaches wins, regardless of declaration order")
    void highestReachedTierWins() {
        assertThat(policy.discountFor(orderWorth("200.00"))).isEqualTo(Money.of("10.00", "EUR"));
        assertThat(policy.discountFor(orderWorth("499.99"))).isEqualTo(Money.of("25.00", "EUR"));
        assertThat(policy.discountFor(orderWorth("500.00"))).isEqualTo(Money.of("50.00", "EUR"));
        assertThat(policy.discountFor(orderWorth("1000.00"))).isEqualTo(Money.of("100.00", "EUR"));
    }

    @Test
    @DisplayName("a policy with no tiers is the same as no discount")
    void emptyPolicyChargesListPrice() {
        IDiscountPolicy empty = new TieredVolumeDiscountPolicy(List.of());

        assertThat(empty.discountFor(orderWorth("1000.00"))).isEqualTo(Money.zero(EUR));
    }

    /** Builds a draft whose subtotal is exactly {@code amount}, using a single unit-priced line. */
    private static Order orderWorth(String amount) {
        Order order = Order.draft(CustomerId.of(UUID.randomUUID()), EUR,
                Address.of("Kerkstraat 1", "Amsterdam", "1012 AB", "NL"), Instant.parse("2026-01-15T10:00:00Z"));
        order.addLine(ProductId.of(UUID.randomUUID()), "Widget", Money.of(amount, "EUR"), Quantity.of(1));
        return order;
    }
}
