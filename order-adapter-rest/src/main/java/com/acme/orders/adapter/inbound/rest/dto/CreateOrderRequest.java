package com.acme.orders.adapter.inbound.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * The HTTP wire format for creating an order.
 *
 * <p>Separate from the application's command on purpose: this is a published contract that has to
 * stay backward compatible for clients, while the command is free to follow the model. Bean Validation
 * here catches malformed requests at the edge, where a 400 is the right answer; the rules that matter
 * to the business are still enforced in the domain, not here.
 */
public record CreateOrderRequest(
        @NotNull(message = "customerId is required") UUID customerId,
        @NotBlank(message = "currency is required") @Size(min = 3, max = 3, message = "currency must be an ISO 4217 code") String currency,
        @NotNull(message = "shippingAddress is required") @Valid AddressPayload shippingAddress) {
}
