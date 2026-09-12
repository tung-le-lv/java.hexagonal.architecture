package com.acme.orders.domain.valueobject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle of an order, with the legal transitions held next to the states themselves.
 *
 * <p>Keeping the transition table here rather than scattering {@code if (status == ...)} checks
 * across services means the lifecycle can be read in one place and cannot drift.
 */
public enum OrderStatus {

    /** Being assembled; the only status in which lines may be edited. */
    DRAFT,
    /** Submitted by the customer and priced; awaiting payment. */
    PLACED,
    /** Paid for; awaiting fulfilment. */
    PAID,
    /** Handed to the carrier. */
    SHIPPED,
    /** Received by the customer. Terminal. */
    DELIVERED,
    /** Abandoned before shipment. Terminal. */
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(DRAFT, EnumSet.of(PLACED, CANCELLED));
        TRANSITIONS.put(PLACED, EnumSet.of(PAID, CANCELLED));
        TRANSITIONS.put(PAID, EnumSet.of(SHIPPED, CANCELLED));
        TRANSITIONS.put(SHIPPED, EnumSet.of(DELIVERED));
        TRANSITIONS.put(DELIVERED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    /** True while the order's lines and shipping details may still be edited. */
    public boolean isEditable() {
        return this == DRAFT;
    }

    /** True once no further transition is possible. */
    public boolean isTerminal() {
        return TRANSITIONS.get(this).isEmpty();
    }

    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public Set<OrderStatus> allowedTransitions() {
        return Collections.unmodifiableSet(TRANSITIONS.get(this));
    }
}
