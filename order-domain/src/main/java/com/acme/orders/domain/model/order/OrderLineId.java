package com.acme.orders.domain.model.order;

import java.util.Objects;
import java.util.UUID;

/** Identity of an {@link OrderLine}, unique within its owning {@link Order}. */
public record OrderLineId(UUID value) {

    public OrderLineId {
        Objects.requireNonNull(value, "order line id must not be null");
    }

    public static OrderLineId newId() {
        return new OrderLineId(UUID.randomUUID());
    }

    public static OrderLineId of(UUID value) {
        return new OrderLineId(value);
    }

    public static OrderLineId of(String value) {
        return new OrderLineId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
