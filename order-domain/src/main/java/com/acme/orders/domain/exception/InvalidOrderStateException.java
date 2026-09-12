package com.acme.orders.domain.exception;

import com.acme.orders.domain.model.order.OrderStatus;

/** Raised when a command is issued against an order whose status does not permit it. */
public class InvalidOrderStateException extends DomainException {

    public InvalidOrderStateException(String operation, OrderStatus current, OrderStatus... allowed) {
        super("order.invalid_state",
                "cannot " + operation + " an order in status " + current + "; allowed: " + java.util.Arrays.toString(allowed));
    }
}
