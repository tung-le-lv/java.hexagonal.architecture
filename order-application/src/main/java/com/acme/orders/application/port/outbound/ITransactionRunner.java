package com.acme.orders.application.port.outbound;

import java.util.function.Supplier;

/**
 * Driven port for the transaction boundary.
 *
 * <p>The use case knows where the boundary belongs — one command, one transaction — but not which
 * technology enforces it, so the boundary is expressed as a port and implemented by the persistence
 * adapter. This is why no use case carries {@code @Transactional}: annotating them would put a
 * Spring dependency in the middle of the hexagon and make the boundary invisible at the call site,
 * where it matters most.
 */
public interface ITransactionRunner {

    <T> T inTransaction(Supplier<T> work);

    default void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
