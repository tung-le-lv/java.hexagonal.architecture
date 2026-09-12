package com.acme.orders.adapter.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A postal address as it appears in JSON, in both requests and responses. */
public record AddressPayload(
        @NotBlank(message = "street is required") String street,
        @NotBlank(message = "city is required") String city,
        @NotBlank(message = "postalCode is required") String postalCode,
        @NotBlank(message = "countryCode is required")
        @Size(min = 2, max = 2, message = "countryCode must be an ISO 3166-1 alpha-2 code") String countryCode) {
}
