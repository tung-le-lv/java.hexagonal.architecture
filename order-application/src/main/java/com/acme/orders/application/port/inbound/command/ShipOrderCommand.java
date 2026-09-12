package com.acme.orders.application.port.inbound.command;

import java.util.UUID;

/** Intent to record that a paid order has been handed to the carrier. */
public record ShipOrderCommand(UUID orderId, String trackingNumber) {
}
