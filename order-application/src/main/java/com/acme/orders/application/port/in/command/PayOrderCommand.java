package com.acme.orders.application.port.in.command;

import java.util.UUID;

/** Intent to record confirmed payment against a placed order. */
public record PayOrderCommand(UUID orderId, String paymentReference) {
}
