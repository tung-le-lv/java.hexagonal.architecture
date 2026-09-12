package com.acme.orders.domain.event;

import com.acme.orders.domain.model.shared.DomainEvent;
import com.acme.orders.domain.model.shared.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The customer committed to the order. Carries a snapshot of what was bought and at what price,
 * so downstream consumers (fulfilment, invoicing, analytics) need not call back for it.
 */
public record OrderPlaced(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID customerId,
        Money subtotal,
        Money discount,
        Money total,
        List<PlacedLine> lines) implements DomainEvent {

    public OrderPlaced {
        lines = List.copyOf(lines);
    }

    public record PlacedLine(UUID productId, String productName, int quantity, Money unitPrice, Money lineTotal) {
    }

    public static OrderPlaced of(UUID orderId, UUID customerId, Money subtotal, Money discount, Money total,
                                 List<PlacedLine> lines, Instant occurredAt) {
        return new OrderPlaced(UUID.randomUUID(), occurredAt, orderId, customerId, subtotal, discount, total, lines);
    }

    @Override
    public String aggregateId() {
        return orderId.toString();
    }
}
