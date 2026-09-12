package com.acme.orders.application.port.inbound;

/**
 * Driving port: pushes recorded-but-undelivered events out to the broker.
 *
 * <p>An input port rather than a background job buried in an adapter, because "events that were
 * recorded must eventually be delivered" is a guarantee the application makes. Whatever triggers it —
 * a scheduler, a queue worker, an admin endpoint, a test — is then just another driving adapter.
 */
public interface IRelayPendingEventsUseCase {

    /**
     * Attempts to relay up to {@code batchSize} events.
     *
     * @return how many were delivered successfully
     */
    int relayPendingEvents(int batchSize);
}
