package com.acme.orders.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

/** The wire format for recording a dispatch. */
public record ShipOrderRequest(@NotBlank(message = "trackingNumber is required") String trackingNumber) {
}
