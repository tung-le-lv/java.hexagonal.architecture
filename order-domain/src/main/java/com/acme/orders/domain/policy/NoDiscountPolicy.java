package com.acme.orders.domain.policy;

import com.acme.orders.domain.model.order.Order;
import com.acme.orders.domain.model.shared.Money;

/** Charges list price. The neutral element, useful as a default and in tests. */
public final class NoDiscountPolicy implements DiscountPolicy {

    @Override
    public Money discountFor(Order order) {
        return Money.zero(order.currency());
    }
}
