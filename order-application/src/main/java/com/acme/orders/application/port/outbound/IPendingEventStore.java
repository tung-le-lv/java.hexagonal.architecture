package com.acme.orders.application.port.outbound;

import java.util.List;
import java.util.UUID;

/**
 * Output port over the store of events awaiting delivery — the read side of the outbox.
 *
 * <p>Separate from {@link IDomainEventPublisher}, which only records events: recording happens inside
 * a business transaction, relaying happens afterwards on its own schedule, and the two have no reason
 * to share an interface.
 */
public interface IPendingEventStore {

    /** Oldest-first batch of events not yet delivered. */
    List<PendingEventMessage> nextBatch(int batchSize);

    void markPublished(UUID messageId);

    void markFailed(UUID messageId, String error);

    /** Backlog size, for health checks and alerting. */
    long pendingCount();
}
