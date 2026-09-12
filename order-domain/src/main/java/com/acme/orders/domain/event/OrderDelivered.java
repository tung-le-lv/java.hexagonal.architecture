package com.acme.orders.domain.event;

import com.acme.orders.domain.model.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/** The customer has received the order. */
public record OrderDelivered(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID customerId) implements DomainEvent {

    public static OrderDelivered of(UUID orderId, UUID customerId, Instant occurredAt) {
        return new OrderDelivered(UUID.randomUUID(), occurredAt, orderId, customerId);
    }

    @Override
    public String aggregateId() {
        return orderId.toString();
    }
}
