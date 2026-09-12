package com.acme.orders.domain.exception;

/** Raised when a line referenced by a command does not belong to the order. */
public class OrderLineNotFoundException extends DomainException {

    public OrderLineNotFoundException(String orderId, String lineId) {
        super("order.line_not_found", "order " + orderId + " has no line " + lineId);
    }
}
