package com.acme.orders.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The HTTP representation of an order.
 *
 * <p>Built from the application's {@code OrderView}, not from the aggregate, and annotated for
 * Jackson here — where serialisation actually belongs — rather than in the core.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponse(
        UUID id,
        UUID customerId,
        String status,
        String currency,
        AddressPayload shippingAddress,
        List<OrderLinePayload> lines,
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

    public record OrderLinePayload(
            UUID id,
            UUID productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {
    }
}
