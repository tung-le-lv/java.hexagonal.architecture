package com.acme.orders.domain.model.order;

import java.util.Objects;
import java.util.UUID;

/** Identity of a product in the catalogue; referenced across the aggregate boundary by id only. */
public record ProductId(UUID value) {

    public ProductId {
        Objects.requireNonNull(value, "product id must not be null");
    }

    public static ProductId of(UUID value) {
        return new ProductId(value);
    }

    public static ProductId of(String value) {
        return new ProductId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
