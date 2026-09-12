package com.acme.orders.application.port.outbound;

import java.time.Instant;
import java.util.UUID;

/**
 * A recorded domain event that has not yet reached the broker, as the relay sees it.
 *
 * <p>Already serialised: the relay moves bytes and does not need to understand the event, which keeps
 * adding a new event type from touching the relay at all.
 */
public record PendingEventMessage(
        UUID id,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        Instant occurredAt) {
}
