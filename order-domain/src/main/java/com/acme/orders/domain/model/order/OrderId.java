package com.acme.orders.domain.model.order;

import java.util.Objects;
import java.util.UUID;

/** Identity of an {@link Order} aggregate. */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "order id must not be null");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }

    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
