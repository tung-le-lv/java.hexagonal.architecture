package com.acme.orders.application.port.inbound.command;

import java.util.UUID;

/** Intent to set a line's quantity to an exact value. */
public record ChangeOrderLineQuantityCommand(UUID orderId, UUID orderLineId, int quantity) {
}
