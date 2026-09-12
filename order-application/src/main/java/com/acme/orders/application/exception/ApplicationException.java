package com.acme.orders.application.exception;

/**
 * Root of the failures the application layer itself reports, as opposed to rules broken inside the
 * domain (see {@code DomainException}).
 *
 * <p>Like domain exceptions these carry a stable code, so the inbound adapter can map them onto
 * transport semantics without string matching.
 */
public abstract class ApplicationException extends RuntimeException {

    private final String code;

    protected ApplicationException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected ApplicationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
