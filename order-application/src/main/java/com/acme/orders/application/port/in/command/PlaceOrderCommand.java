package com.acme.orders.application.port.in.command;

import java.util.UUID;

/** Intent to submit a draft order for fulfilment. */
public record PlaceOrderCommand(UUID orderId) {
}
