package com.acme.orders.adapter.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;

/** The wire format for recording a payment. */
public record PayOrderRequest(@NotBlank(message = "paymentReference is required") String paymentReference) {
}
