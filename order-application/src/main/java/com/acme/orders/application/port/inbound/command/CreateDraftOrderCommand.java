package com.acme.orders.application.port.inbound.command;

import java.util.UUID;

/**
 * Intent to start a new order.
 *
 * <p>Commands are the application's own vocabulary: primitives in, so no transport format and no
 * persistence type ever reaches the core. Adapters translate into these; the core translates them
 * into value objects.
 */
public record CreateDraftOrderCommand(
        UUID customerId,
        String currencyCode,
        String street,
        String city,
        String postalCode,
        String countryCode) {
}
