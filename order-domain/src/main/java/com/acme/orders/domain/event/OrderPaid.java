package com.acme.orders.domain.event;

import com.acme.orders.domain.model.shared.IDomainEvent;
import com.acme.orders.domain.model.shared.Money;
import java.time.Instant;
import java.util.UUID;

/** Payment for the order has been confirmed. */
public record OrderPaid(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID customerId,
        Money amountPaid,
        String paymentReference) implements IDomainEvent {

    public static OrderPaid of(UUID orderId, UUID customerId, Money amountPaid, String paymentReference, Instant occurredAt) {
        return new OrderPaid(UUID.randomUUID(), occurredAt, orderId, customerId, amountPaid, paymentReference);
    }

    @Override
    public String aggregateId() {
        return orderId.toString();
    }
}
