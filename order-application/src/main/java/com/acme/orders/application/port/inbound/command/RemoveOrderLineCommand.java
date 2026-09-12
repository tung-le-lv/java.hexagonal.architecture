package com.acme.orders.application.port.inbound.command;

import java.util.UUID;

/** Intent to take a line off a draft order. */
public record RemoveOrderLineCommand(UUID orderId, UUID orderLineId) {
}
