package com.acme.orders.domain.valueobject;

import com.acme.orders.domain.exception.InvalidQuantityException;

/** A strictly positive count of items. Zero is expressed by removing the line, not by a zero quantity. */
public record Quantity(int value) implements Comparable<Quantity> {

    private static final int MAX_PER_LINE = 1_000;

    public Quantity {
        if (value <= 0) {
            throw new InvalidQuantityException("quantity must be greater than zero but was " + value);
        }
        if (value > MAX_PER_LINE) {
            throw new InvalidQuantityException("quantity must not exceed " + MAX_PER_LINE + " but was " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public Quantity plus(Quantity other) {
        return new Quantity(value + other.value);
    }

    @Override
    public int compareTo(Quantity other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
