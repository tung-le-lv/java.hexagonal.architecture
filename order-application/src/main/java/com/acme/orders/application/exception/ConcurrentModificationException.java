package com.acme.orders.application.exception;

import java.util.UUID;

/**
 * Another transaction changed the same order first, so this one was rejected rather than allowed to
 * overwrite it. Retrying the command from a fresh read is the normal response.
 */
public class ConcurrentModificationException extends ApplicationException {

    public ConcurrentModificationException(UUID orderId, Throwable cause) {
        super("order.concurrent_modification",
                "order " + orderId + " was modified concurrently; reload it and retry", cause);
    }
}
