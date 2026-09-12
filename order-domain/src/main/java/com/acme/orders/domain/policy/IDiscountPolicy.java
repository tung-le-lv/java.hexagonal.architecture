package com.acme.orders.domain.policy;

import com.acme.orders.domain.model.order.Order;
import com.acme.orders.domain.model.shared.Money;

/**
 * How much comes off an order's subtotal when it is placed.
 *
 * <p>A domain service, not aggregate behaviour: discounting is a business decision that changes
 * independently of the order lifecycle and may depend on more than one order's worth of context.
 * {@link Order#place} takes it as a parameter (double dispatch) rather than reaching out for it, so
 * the aggregate keeps no dependency on whichever policy is in force.
 */
public interface IDiscountPolicy {

    /** Must return a non-negative amount in the order's currency, never exceeding the subtotal. */
    Money discountFor(Order order);
}
