package com.acme.orders.domain.event;

import com.acme.orders.domain.model.order.OrderStatus;
import com.acme.orders.domain.model.shared.IDomainEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * The order was abandoned before shipment. Carries the status it was cancelled from, because
 * whether a refund is owed depends on how far the order had progressed.
 */
public record OrderCancelled(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID customerId,
        OrderStatus cancelledFrom,
        String reason) implements IDomainEvent {

    public static OrderCancelled of(UUID orderId, UUID customerId, OrderStatus cancelledFrom, String reason, Instant occurredAt) {
        return new OrderCancelled(UUID.randomUUID(), occurredAt, orderId, customerId, cancelledFrom, reason);
    }

    @Override
    public String aggregateId() {
        return orderId.toString();
    }
}
