package com.acme.orders.domain.exception;

/** Raised when an order with no lines is placed. An order must represent a real purchase. */
public class EmptyOrderException extends DomainException {

    public EmptyOrderException(String orderId) {
        super("order.empty", "order " + orderId + " has no lines and cannot be placed");
    }
}
