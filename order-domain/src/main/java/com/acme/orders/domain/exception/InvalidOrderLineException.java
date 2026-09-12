package com.acme.orders.domain.exception;

/** Raised when a line's product details or price are not sellable. */
public class InvalidOrderLineException extends DomainException {

    public InvalidOrderLineException(String message) {
        super("order.line_invalid", message);
    }
}
