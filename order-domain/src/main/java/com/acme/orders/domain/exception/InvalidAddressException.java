package com.acme.orders.domain.exception;

/** Raised when an address is incomplete or malformed. */
public class InvalidAddressException extends DomainException {

    public InvalidAddressException(String message) {
        super("address.invalid", message);
    }
}
