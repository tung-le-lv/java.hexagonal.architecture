package com.acme.orders.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The wire format for cancelling an order. */
public record CancelOrderRequest(
        @NotBlank(message = "reason is required")
        @Size(max = 512, message = "reason must not exceed 512 characters") String reason) {
}
