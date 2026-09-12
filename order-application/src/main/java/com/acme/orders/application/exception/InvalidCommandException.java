package com.acme.orders.application.exception;

/**
 * A command was structurally unusable — a missing field, an unparseable currency code — and was
 * rejected before any aggregate was touched.
 */
public class InvalidCommandException extends ApplicationException {

    public InvalidCommandException(String message) {
        super("command.invalid", message);
    }

    public InvalidCommandException(String message, Throwable cause) {
        super("command.invalid", message, cause);
    }
}
