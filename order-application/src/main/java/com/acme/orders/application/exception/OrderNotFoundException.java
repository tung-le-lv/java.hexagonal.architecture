package com.acme.orders.application.exception;

import java.util.UUID;

/** The order a command or query referred to does not exist. */
public class OrderNotFoundException extends ApplicationException {

    public OrderNotFoundException(UUID orderId) {
        super("order.not_found", "order " + orderId + " was not found");
    }
}
