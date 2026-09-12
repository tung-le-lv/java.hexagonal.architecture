package com.acme.orders.domain.model.order;

import static com.acme.orders.domain.model.order.OrderFixtures.EUR;
import static com.acme.orders.domain.model.order.OrderFixtures.T0;
import static com.acme.orders.domain.model.order.OrderFixtures.aDraft;
import static com.acme.orders.domain.model.order.OrderFixtures.aPlacedOrder;
import static com.acme.orders.domain.model.order.OrderFixtures.eur;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.orders.domain.exception.CurrencyMismatchException;
import com.acme.orders.domain.exception.InvalidOrderStateException;
import com.acme.orders.domain.exception.OrderLineNotFoundException;
import com.acme.orders.domain.model.shared.Money;
import com.acme.orders.domain.model.shared.Quantity;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderDraftEditingTest {

    private final ProductId keyboard = ProductId.of(UUID.randomUUID());
    private final ProductId mouse = ProductId.of(UUID.randomUUID());

    @Test
    @DisplayName("a new draft is empty, editable and costs nothing")
    void newDraftIsEmpty() {
        Order order = aDraft();

        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.status().isEditable()).isTrue();
        assertThat(order.lines()).isEmpty();
        assertThat(order.subtotal()).isEqualTo(Money.zero(EUR));
        assertThat(order.total()).isEqualTo(Money.zero(EUR));
        assertThat(order.createdAt()).isEqualTo(T0);
        assertThat(order.placedAt()).isEmpty();
    }

    @Test
    @DisplayName("the total is derived from the lines, so it cannot disagree with them")
    void totalIsDerivedFromLines() {
        Order order = aDraft();

        order.addLine(keyboard, "Mechanical keyboard", eur("79.99"), Quantity.of(2));
        order.addLine(mouse, "Trackball", eur("45.50"), Quantity.of(1));

        assertThat(order.subtotal()).isEqualTo(eur("205.48"));
        assertThat(order.total()).isEqualTo(eur("205.48"));
        assertThat(order.totalItemCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("adding a product that is already on the order merges into the existing line")
    void addingSameProductMergesQuantities() {
        Order order = aDraft();

        OrderLineId first = order.addLine(keyboard, "Mechanical keyboard", eur("50.00"), Quantity.of(2));
        OrderLineId second = order.addLine(keyboard, "Mechanical keyboard", eur("50.00"), Quantity.of(3));

        assertThat(second).isEqualTo(first);
        assertThat(order.lines()).hasSize(1);
        assertThat(order.lines().get(0).quantity()).isEqualTo(Quantity.of(5));
        assertThat(order.subtotal()).isEqualTo(eur("250.00"));
    }

    @Test
    @DisplayName("a line priced in another currency is rejected, not silently added")
    void lineCurrencyMustMatchTheOrder() {
        Order order = aDraft();

        assertThatThrownBy(() -> order.addLine(keyboard, "Mechanical keyboard", Money.of("50.00", "USD"), Quantity.of(1)))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessageContaining("EUR")
                .hasMessageContaining("USD");
        assertThat(order.lines()).isEmpty();
    }

    @Test
    @DisplayName("changing a line's quantity reprices the order")
    void changingQuantityReprices() {
        Order order = aDraft();
        OrderLineId lineId = order.addLine(keyboard, "Mechanical keyboard", eur("50.00"), Quantity.of(1));

        order.changeLineQuantity(lineId, Quantity.of(4));

        assertThat(order.subtotal()).isEqualTo(eur("200.00"));
    }

    @Test
    @DisplayName("removing a line drops it from the total")
    void removingLineReprices() {
        Order order = aDraft();
        OrderLineId keyboardLine = order.addLine(keyboard, "Mechanical keyboard", eur("50.00"), Quantity.of(1));
        order.addLine(mouse, "Trackball", eur("20.00"), Quantity.of(1));

        order.removeLine(keyboardLine);

        assertThat(order.lines()).hasSize(1);
        assertThat(order.subtotal()).isEqualTo(eur("20.00"));
    }

    @Test
    @DisplayName("a command naming a line that is not on the order is rejected")
    void unknownLineIsRejected() {
        Order order = aDraft();

        assertThatThrownBy(() -> order.changeLineQuantity(OrderLineId.newId(), Quantity.of(2)))
                .isInstanceOf(OrderLineNotFoundException.class);
    }

    @Test
    @DisplayName("lines cannot be edited once the order has left draft")
    void linesAreFrozenAfterPlacement() {
        Order order = aPlacedOrder();
        OrderLineId existingLine = order.lines().get(0).id();

        assertThatThrownBy(() -> order.addLine(mouse, "Trackball", eur("20.00"), Quantity.of(1)))
                .isInstanceOf(InvalidOrderStateException.class);
        assertThatThrownBy(() -> order.changeLineQuantity(existingLine, Quantity.of(9)))
                .isInstanceOf(InvalidOrderStateException.class);
        assertThatThrownBy(() -> order.removeLine(existingLine))
                .isInstanceOf(InvalidOrderStateException.class);
        assertThatThrownBy(() -> order.changeShippingAddress(OrderFixtures.anAddress()))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    @DisplayName("the line list handed out cannot be used to bypass the aggregate")
    void linesViewIsImmutable() {
        Order order = aDraft();
        order.addLine(keyboard, "Mechanical keyboard", eur("50.00"), Quantity.of(1));

        assertThatThrownBy(() -> order.lines().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
