package com.acme.orders.domain.exception;

/** Raised when two monetary amounts in different currencies are combined or compared. */
public class CurrencyMismatchException extends DomainException {

    public CurrencyMismatchException(String expected, String actual) {
        super("currency.mismatch", "expected currency " + expected + " but got " + actual);
    }
}
