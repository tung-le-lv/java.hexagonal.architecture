package com.acme.orders.domain.model.order;

import static com.acme.orders.domain.model.order.OrderFixtures.EUR;
import static com.acme.orders.domain.model.order.OrderFixtures.T0;
import static com.acme.orders.domain.model.order.OrderFixtures.aDraft;
import static com.acme.orders.domain.model.order.OrderFixtures.aDraftWithOneLine;
import static com.acme.orders.domain.model.order.OrderFixtures.aPaidOrder;
import static com.acme.orders.domain.model.order.OrderFixtures.aPlacedOrder;
import static com.acme.orders.domain.model.order.OrderFixtures.eur;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.orders.domain.event.OrderCancelled;
import com.acme.orders.domain.event.OrderPaid;
import com.acme.orders.domain.event.OrderPlaced;
import com.acme.orders.domain.event.OrderShipped;
import com.acme.orders.domain.exception.EmptyOrderException;
import com.acme.orders.domain.exception.InvalidOrderStateException;
import com.acme.orders.domain.exception.MissingOrderDetailException;
import com.acme.orders.domain.exception.OrderLimitExceededException;
import com.acme.orders.domain.model.shared.DomainEvent;
import com.acme.orders.domain.model.shared.Money;
import com.acme.orders.domain.policy.DiscountPolicy;
import com.acme.orders.domain.policy.NoDiscountPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderLifecycleTest {

    private static final DiscountPolicy NO_DISCOUNT = new NoDiscountPolicy();
    private static final Instant PLACED_AT = T0.plusSeconds(60);

    @Test
    @DisplayName("placing an order records what was bought, at what price, and when")
    void placingRecordsAnEvent() {
        Order order = aDraftWithOneLine();

        order.place(NO_DISCOUNT, PLACED_AT);

        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.placedAt()).contains(PLACED_AT);

        List<DomainEvent> events = order.pendingEvents();
        assertThat(events).hasSize(1);
        OrderPlaced placed = (OrderPlaced) events.get(0);
        assertThat(placed.orderId()).isEqualTo(order.id().value());
        assertThat(placed.occurredAt()).isEqualTo(PLACED_AT);
        assertThat(placed.total()).isEqualTo(eur("100.00"));
        assertThat(placed.lines()).singleElement()
                .satisfies(line -> {
                    assertThat(line.quantity()).isEqualTo(2);
                    assertThat(line.lineTotal()).isEqualTo(eur("100.00"));
                });
    }

    @Test
    @DisplayName("an empty order cannot be placed")
    void emptyOrderCannotBePlaced() {
        Order order = aDraft();

        assertThatThrownBy(() -> order.place(NO_DISCOUNT, PLACED_AT))
                .isInstanceOf(EmptyOrderException.class);
        assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.pendingEvents()).isEmpty();
    }

    @Test
    @DisplayName("the discount the policy returns is applied to the total")
    void discountIsAppliedAtPlacement() {
        Order order = aDraftWithOneLine();

        order.place(o -> eur("15.00"), PLACED_AT);

        assertThat(order.discount()).isEqualTo(eur("15.00"));
        assertThat(order.subtotal()).isEqualTo(eur("100.00"));
        assertThat(order.total()).isEqualTo(eur("85.00"));
    }

    @Test
    @DisplayName("a policy that returns a nonsensical discount is rejected by the order, not trusted")
    void aggregateValidatesTheDiscount() {
        assertThatThrownBy(() -> aDraftWithOneLine().place(o -> eur("100.01"), PLACED_AT))
                .isInstanceOf(OrderLimitExceededException.class)
                .hasMessageContaining("exceed");

        assertThatThrownBy(() -> aDraftWithOneLine().place(o -> eur("-1.00"), PLACED_AT))
                .isInstanceOf(OrderLimitExceededException.class)
                .hasMessageContaining("negative");

        assertThatThrownBy(() -> aDraftWithOneLine().place(o -> Money.of(BigDecimal.ONE, OrderFixtures.USD), PLACED_AT))
                .isInstanceOf(com.acme.orders.domain.exception.CurrencyMismatchException.class);
    }

    @Test
    @DisplayName("an order cannot be placed twice")
    void placingIsNotIdempotent() {
        Order order = aPlacedOrder();

        assertThatThrownBy(() -> order.place(NO_DISCOUNT, PLACED_AT))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("PLACED");
    }

    @Test
    @DisplayName("paying a placed order records the amount actually owed")
    void payingRecordsTheAmountOwed() {
        Order order = aPlacedOrder();
        Instant paidAt = PLACED_AT.plusSeconds(30);

        order.pay("stripe-ch-123", paidAt);

        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.paymentReference()).contains("stripe-ch-123");
        OrderPaid paid = (OrderPaid) order.pendingEvents().get(0);
        assertThat(paid.amountPaid()).isEqualTo(order.total());
        assertThat(paid.occurredAt()).isEqualTo(paidAt);
    }

    @Test
    @DisplayName("an unpaid order cannot ship")
    void shippingRequiresPayment() {
        Order order = aPlacedOrder();

        assertThatThrownBy(() -> order.ship("TRACK-1", PLACED_AT.plusSeconds(30)))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    @DisplayName("shipping carries the tracking number and destination")
    void shippingRecordsTracking() {
        Order order = aPaidOrder();

        order.ship("TRACK-1", T0.plusSeconds(300));

        assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        OrderShipped shipped = (OrderShipped) order.pendingEvents().get(0);
        assertThat(shipped.trackingNumber()).isEqualTo("TRACK-1");
        assertThat(shipped.shippingAddress()).isEqualTo(order.shippingAddress());
    }

    @Test
    @DisplayName("a transition that needs a detail is refused without it")
    void transitionsRequireTheirDetails() {
        assertThatThrownBy(() -> aPlacedOrder().pay("  ", T0))
                .isInstanceOf(MissingOrderDetailException.class);
        assertThatThrownBy(() -> aPaidOrder().ship(null, T0))
                .isInstanceOf(MissingOrderDetailException.class);
        assertThatThrownBy(() -> aPlacedOrder().cancel("", T0))
                .isInstanceOf(MissingOrderDetailException.class);
    }

    @Test
    @DisplayName("cancelling records the status it was cancelled from, since that decides refunds")
    void cancellingRecordsWhereItCameFrom() {
        Order order = aPaidOrder();

        order.cancel("customer changed their mind", T0.plusSeconds(400));

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.status().isTerminal()).isTrue();
        OrderCancelled cancelled = (OrderCancelled) order.pendingEvents().get(0);
        assertThat(cancelled.cancelledFrom()).isEqualTo(OrderStatus.PAID);
        assertThat(cancelled.reason()).isEqualTo("customer changed their mind");
    }

    @Test
    @DisplayName("a shipped order cannot be cancelled; that is a return, not a cancellation")
    void shippedOrdersCannotBeCancelled() {
        Order order = aPaidOrder();
        order.ship("TRACK-1", T0.plusSeconds(300));

        assertThatThrownBy(() -> order.cancel("too late", T0.plusSeconds(400)))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    @DisplayName("delivery closes the order for good")
    void deliveryIsTerminal() {
        Order order = aPaidOrder();
        order.ship("TRACK-1", T0.plusSeconds(300));
        order.drainEvents();

        order.markDelivered(T0.plusSeconds(900));

        assertThat(order.status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.status().isTerminal()).isTrue();
        assertThatThrownBy(() -> order.cancel("nope", T0.plusSeconds(1000)))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    @DisplayName("events are handed over once, so they cannot be published twice")
    void drainingEventsEmptiesTheBuffer() {
        Order order = aDraftWithOneLine();
        order.place(NO_DISCOUNT, PLACED_AT);

        assertThat(order.drainEvents()).hasSize(1);
        assertThat(order.drainEvents()).isEmpty();
        assertThat(order.pendingEvents()).isEmpty();
    }

    @Test
    @DisplayName("a rejected command leaves no trace")
    void failedCommandsRaiseNoEvents() {
        Order order = aPlacedOrder();

        assertThatThrownBy(() -> order.ship("TRACK-1", T0)).isInstanceOf(InvalidOrderStateException.class);

        assertThat(order.pendingEvents()).isEmpty();
        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.trackingNumber()).isEmpty();
    }

    @Test
    @DisplayName("two orders are the same order when their identities match")
    void identityIsByIdAlone() {
        Order order = aDraftWithOneLine();
        Order sameIdentity = Order.rehydrate(order.toSnapshot());
        Order other = aDraftWithOneLine();

        assertThat(order).isEqualTo(sameIdentity).hasSameHashCodeAs(sameIdentity);
        assertThat(order).isNotEqualTo(other);
    }

    @Test
    @DisplayName("a rehydrated order carries its state but raises no events")
    void rehydrationIsNotABusinessDecision() {
        Order placed = aPlacedOrder();

        Order restored = Order.rehydrate(placed.toSnapshot());

        assertThat(restored.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(restored.total()).isEqualTo(placed.total());
        assertThat(restored.lines()).hasSize(placed.lines().size());
        assertThat(restored.currency()).isEqualTo(EUR);
        assertThat(restored.pendingEvents()).isEmpty();
    }
}
