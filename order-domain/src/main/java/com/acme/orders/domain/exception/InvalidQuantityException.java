package com.acme.orders.domain.exception;

/** Raised when a quantity falls outside the range the domain allows. */
public class InvalidQuantityException extends DomainException {

    public InvalidQuantityException(String message) {
        super("quantity.invalid", message);
    }
}
