package com.acme.orders.domain.policy;

import com.acme.orders.domain.model.order.Order;
import com.acme.orders.domain.model.shared.Money;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Rewards larger baskets: the highest tier whose threshold the subtotal reaches is applied.
 *
 * <p>Thresholds are expressed as {@link Money}, so a policy instance is bound to one currency and
 * an order in a different currency is rejected by {@link Money}'s own arithmetic rather than
 * silently mispriced.
 */
public final class TieredVolumeDiscountPolicy implements IDiscountPolicy {

    private final List<Tier> tiers;

    public record Tier(Money threshold, BigDecimal percentage) {

        public Tier {
            Objects.requireNonNull(threshold, "threshold must not be null");
            Objects.requireNonNull(percentage, "percentage must not be null");
            if (percentage.signum() < 0 || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("percentage must be between 0 and 100 but was " + percentage);
            }
        }
    }

    public TieredVolumeDiscountPolicy(List<Tier> tiers) {
        this.tiers = List.copyOf(tiers).stream()
                .sorted(Comparator.comparing(Tier::threshold).reversed())
                .toList();
    }

    @Override
    public Money discountFor(Order order) {
        Money subtotal = order.subtotal();
        return tiers.stream()
                .filter(tier -> !subtotal.isLessThan(tier.threshold()))
                .findFirst()
                .map(tier -> subtotal.percentage(tier.percentage()))
                .orElseGet(() -> Money.zero(order.currency()));
    }
}
