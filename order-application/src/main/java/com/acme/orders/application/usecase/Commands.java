package com.acme.orders.application.usecase;

import com.acme.orders.application.exception.InvalidCommandException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

/**
 * Turns the primitives that arrive on a command into the types the domain speaks.
 *
 * <p>This is the translation seam: anything structurally unusable is rejected here as an
 * {@link InvalidCommandException} before an aggregate is loaded, so the domain only ever sees values
 * it can make sense of and never has to defend against nulls from the wire.
 */
final class Commands {

    private Commands() {
    }

    static UUID requireId(UUID value, String field) {
        if (value == null) {
            throw new InvalidCommandException(field + " must not be null");
        }
        return value;
    }

    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidCommandException(field + " must not be blank");
        }
        return value.strip();
    }

    static BigDecimal requireAmount(BigDecimal value, String field) {
        if (value == null) {
            throw new InvalidCommandException(field + " must not be null");
        }
        return value;
    }

    static Currency requireCurrency(String currencyCode) {
        String code = requireText(currencyCode, "currencyCode");
        try {
            return Currency.getInstance(code.toUpperCase());
        } catch (IllegalArgumentException cause) {
            throw new InvalidCommandException("unknown currency code " + currencyCode, cause);
        }
    }
}
