package com.acme.orders.adapter.inbound.rest.dto;

import jakarta.validation.constraints.Positive;

/** The wire format for setting a line's quantity. */
public record ChangeQuantityRequest(@Positive(message = "quantity must be greater than zero") int quantity) {
}
