package com.acme.orders.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A read model of an order, flat and serialisation-friendly.
 *
 * <p>Returning this instead of the {@link com.acme.orders.domain.aggregate.Order} aggregate keeps
 * the aggregate out of HTTP responses: inbound adapters cannot accidentally couple to the model's
 * shape, and the model can be refactored without breaking the API.
 */
public record OrderView(
        UUID orderId,
        UUID customerId,
        String status,
        String currency,
        AddressView shippingAddress,
        List<OrderLineView> lines,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        int totalItemCount,
        Instant createdAt,
        Instant placedAt,
        Instant paidAt,
        Instant shippedAt,
        Instant cancelledAt,
        String cancellationReason,
        String paymentReference,
        String trackingNumber,
        long version) {

    public OrderView {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public record OrderLineView(
            UUID lineId,
            UUID productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {
    }

    public record AddressView(String street, String city, String postalCode, String countryCode) {
    }
}
