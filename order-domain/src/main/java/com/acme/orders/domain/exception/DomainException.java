package com.acme.orders.domain.exception;

/**
 * Root of every rule violation the domain can signal.
 *
 * <p>Domain exceptions carry a stable machine-readable code so adapters can translate them into
 * transport-specific errors without matching on class names or message text.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
