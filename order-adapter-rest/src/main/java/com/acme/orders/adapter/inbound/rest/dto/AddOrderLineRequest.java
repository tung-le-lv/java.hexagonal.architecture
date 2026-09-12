package com.acme.orders.adapter.inbound.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/** The wire format for adding a line to a draft order. */
public record AddOrderLineRequest(
        @NotNull(message = "productId is required") UUID productId,
        @NotBlank(message = "productName is required") String productName,
        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.00", message = "unitPrice must not be negative")
        @Digits(integer = 15, fraction = 4, message = "unitPrice has too many digits") BigDecimal unitPrice,
        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be an ISO 4217 code") String currency,
        @Positive(message = "quantity must be greater than zero") int quantity) {
}
