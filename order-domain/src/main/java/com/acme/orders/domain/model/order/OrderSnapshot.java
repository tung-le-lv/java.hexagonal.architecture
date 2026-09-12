package com.acme.orders.domain.model.order;

import com.acme.orders.domain.model.shared.Address;
import com.acme.orders.domain.model.shared.Money;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

/**
 * The complete state of an {@link Order} as plain data.
 *
 * <p>A memento, and the only way state crosses the aggregate boundary in bulk: persistence adapters
 * read it via {@link Order#toSnapshot()} and write it via {@link Order#rehydrate}. That leaves the
 * aggregate with no setters, no public constructor and no framework annotations, while still being
 * fully mappable — the alternatives (reflection, or opening the model up) both cost more.
 */
public record OrderSnapshot(
        OrderId id,
        CustomerId customerId,
        OrderStatus status,
        Currency currency,
        Address shippingAddress,
        List<OrderLine> lines,
        Money discount,
        Instant createdAt,
        Instant placedAt,
        Instant paidAt,
        Instant shippedAt,
        Instant cancelledAt,
        String cancellationReason,
        String paymentReference,
        String trackingNumber,
        long version) {

    public OrderSnapshot {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    /** Returns a copy with the version the store reports after a write. */
    public OrderSnapshot withVersion(long newVersion) {
        return new OrderSnapshot(id, customerId, status, currency, shippingAddress, lines, discount, createdAt,
                placedAt, paidAt, shippedAt, cancelledAt, cancellationReason, paymentReference, trackingNumber,
                newVersion);
    }
}
