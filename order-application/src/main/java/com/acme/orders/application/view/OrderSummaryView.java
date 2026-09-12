package com.acme.orders.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A list-page projection: enough to render a row, without loading every line. */
public record OrderSummaryView(
        UUID orderId,
        UUID customerId,
        String status,
        String currency,
        BigDecimal total,
        int lineCount,
        Instant createdAt,
        Instant placedAt) {
}
