package com.acme.orders.domain.event;

import com.acme.orders.domain.model.shared.Address;
import com.acme.orders.domain.model.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/** The order has been handed to the carrier. */
public record OrderShipped(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID customerId,
        String trackingNumber,
        Address shippingAddress) implements DomainEvent {

    public static OrderShipped of(UUID orderId, UUID customerId, String trackingNumber, Address shippingAddress, Instant occurredAt) {
        return new OrderShipped(UUID.randomUUID(), occurredAt, orderId, customerId, trackingNumber, shippingAddress);
    }

    @Override
    public String aggregateId() {
        return orderId.toString();
    }
}
