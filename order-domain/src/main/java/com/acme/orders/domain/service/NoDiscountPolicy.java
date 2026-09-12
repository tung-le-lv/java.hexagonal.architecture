package com.acme.orders.domain.service;

import com.acme.orders.domain.aggregate.Order;
import com.acme.orders.domain.valueobject.Money;

/** Charges list price. The neutral element, useful as a default and in tests. */
public final class NoDiscountPolicy implements IDiscountPolicy {

    @Override
    public Money discountFor(Order order) {
        return Money.zero(order.currency());
    }
}
