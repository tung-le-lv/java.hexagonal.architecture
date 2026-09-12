package com.acme.orders.application.port.in.command;

import java.math.BigDecimal;
import java.util.UUID;

/** Intent to put a product on a draft order, or to increase its quantity if already there. */
public record AddOrderLineCommand(
        UUID orderId,
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        String currencyCode,
        int quantity) {
}
