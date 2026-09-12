package com.acme.orders.domain.model.order;

import com.acme.orders.domain.model.shared.Address;
import com.acme.orders.domain.model.shared.Money;
import com.acme.orders.domain.model.shared.Quantity;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

/** Test data builders, so each test states only what it actually cares about. */
final class OrderFixtures {

    static final Currency EUR = Currency.getInstance("EUR");
    static final Currency USD = Currency.getInstance("USD");
    static final Instant T0 = Instant.parse("2026-01-15T10:00:00Z");

    private OrderFixtures() {
    }

    static Address anAddress() {
        return Address.of("Kerkstraat 1", "Amsterdam", "1012 AB", "NL");
    }

    static Order aDraft() {
        return Order.draft(CustomerId.of(UUID.randomUUID()), EUR, anAddress(), T0);
    }

    /** A draft with one line worth 100.00 EUR. */
    static Order aDraftWithOneLine() {
        Order order = aDraft();
        order.addLine(ProductId.of(UUID.randomUUID()), "Mechanical keyboard", eur("50.00"), Quantity.of(2));
        return order;
    }

    static Order aPlacedOrder() {
        Order order = aDraftWithOneLine();
        order.place(o -> Money.zero(EUR), T0.plusSeconds(60));
        order.drainEvents();
        return order;
    }

    static Order aPaidOrder() {
        Order order = aPlacedOrder();
        order.pay("pay-ref-1", T0.plusSeconds(120));
        order.drainEvents();
        return order;
    }

    static Money eur(String amount) {
        return Money.of(amount, "EUR");
    }
}
