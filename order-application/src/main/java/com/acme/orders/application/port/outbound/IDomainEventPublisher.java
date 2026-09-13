package com.acme.orders.application.port.outbound;

import com.acme.orders.domain.event.IDomainEvent;
import java.util.List;

/**
 * Output port for getting domain events out of the service.
 *
 * <p>The application calls this inside the same transaction as the state change it describes; how
 * that is made reliable — an outbox table, a broker transaction — is the adapter's problem. The core
 * states only that the events must not be lost if the write commits.
 */
public interface IDomainEventPublisher {

    void publish(List<IDomainEvent> events);
}
