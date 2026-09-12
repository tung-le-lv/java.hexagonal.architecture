package com.acme.orders.adapter.inbound.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A row in the order list. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderSummaryResponse(
        UUID id,
        UUID customerId,
        String status,
        String currency,
        BigDecimal total,
        int lineCount,
        Instant createdAt,
        Instant placedAt) {
}
