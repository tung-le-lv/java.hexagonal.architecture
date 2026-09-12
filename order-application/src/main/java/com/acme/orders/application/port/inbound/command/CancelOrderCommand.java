package com.acme.orders.application.port.inbound.command;

import java.util.UUID;

/** Intent to abandon an order that has not shipped. */
public record CancelOrderCommand(UUID orderId, String reason) {
}
