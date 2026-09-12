package com.acme.orders.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.orders.application.exception.ConcurrentModificationException;
import com.acme.orders.application.exception.InvalidCommandException;
import com.acme.orders.application.exception.OrderNotFoundException;
import com.acme.orders.application.fake.DirectTransactionRunner;
import com.acme.orders.application.fake.InMemoryOrderRepository;
import com.acme.orders.application.fake.RecordingEventPublisher;
import com.acme.orders.application.port.inbound.command.AddOrderLineCommand;
import com.acme.orders.application.port.inbound.command.CancelOrderCommand;
import com.acme.orders.application.port.inbound.command.ChangeOrderLineQuantityCommand;
import com.acme.orders.application.port.inbound.command.CreateDraftOrderCommand;
import com.acme.orders.application.port.inbound.command.PayOrderCommand;
import com.acme.orders.application.port.inbound.command.PlaceOrderCommand;
import com.acme.orders.application.port.inbound.command.RemoveOrderLineCommand;
import com.acme.orders.application.port.inbound.command.ShipOrderCommand;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.domain.event.OrderCancelled;
import com.acme.orders.domain.event.OrderPaid;
import com.acme.orders.domain.event.OrderPlaced;
import com.acme.orders.domain.exception.InvalidOrderStateException;
import com.acme.orders.domain.exception.InvalidQuantityException;
import com.acme.orders.domain.model.order.Order;
import com.acme.orders.domain.model.order.OrderId;
import com.acme.orders.domain.model.order.ProductId;
import com.acme.orders.domain.model.shared.Money;
import com.acme.orders.domain.model.shared.Quantity;
import com.acme.orders.domain.policy.IDiscountPolicy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises the use cases against fake ports.
 *
 * <p>No Spring, no database, no mocking framework — the hexagon's whole purpose, demonstrated. Time is
 * frozen through the injected {@link Clock}, so assertions on timestamps are exact.
 */
class OrderUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");
    private static final UUID CUSTOMER = UUID.randomUUID();
    private static final IDiscountPolicy FLAT_TEN_PERCENT =
            order -> order.subtotal().percentage(BigDecimal.TEN);

    private InMemoryOrderRepository orders;
    private RecordingEventPublisher events;
    private DirectTransactionRunner transactions;
    private Clock clock;

    private CreateDraftOrderService createDraftOrder;
    private AddOrderLineService addOrderLine;
    private ChangeOrderLineQuantityService changeQuantity;
    private RemoveOrderLineService removeOrderLine;
    private PlaceOrderService placeOrder;
    private PayOrderService payOrder;
    private ShipOrderService shipOrder;
    private CancelOrderService cancelOrder;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrderRepository();
        events = new RecordingEventPublisher();
        transactions = new DirectTransactionRunner();
        clock = Clock.fixed(NOW, ZoneOffset.UTC);

        createDraftOrder = new CreateDraftOrderService(orders, events, transactions, clock);
        addOrderLine = new AddOrderLineService(orders, events, transactions);
        changeQuantity = new ChangeOrderLineQuantityService(orders, events, transactions);
        removeOrderLine = new RemoveOrderLineService(orders, events, transactions);
        placeOrder = new PlaceOrderService(orders, events, transactions, FLAT_TEN_PERCENT, clock);
        payOrder = new PayOrderService(orders, events, transactions, clock);
        shipOrder = new ShipOrderService(orders, events, transactions, clock);
        cancelOrder = new CancelOrderService(orders, events, transactions, clock);
    }

    @Test
    @DisplayName("creating a draft persists it and stamps it with the injected clock")
    void createsADraft() {
        OrderView view = createDraftOrder.createDraftOrder(createCommand());

        assertThat(view.status()).isEqualTo("DRAFT");
        assertThat(view.customerId()).isEqualTo(CUSTOMER);
        assertThat(view.currency()).isEqualTo("EUR");
        assertThat(view.createdAt()).isEqualTo(NOW);
        assertThat(view.total()).isEqualByComparingTo("0.00");
        assertThat(orders.size()).isEqualTo(1);
        assertThat(events.published()).isEmpty();
        assertThat(transactions.invocations()).isEqualTo(1);
    }

    @Test
    @DisplayName("the version the store assigned is reflected back to the caller, and advances on each write")
    void returnsThePersistedVersion() {
        OrderView created = createDraftOrder.createDraftOrder(createCommand());
        assertThat(created.version()).isZero();

        OrderView afterFirstWrite = addOrderLine.addOrderLine(lineCommand(created.orderId(), "Widget", "10.00", 1));
        assertThat(afterFirstWrite.version()).isEqualTo(1L);

        OrderView afterSecondWrite = placeOrder.placeOrder(new PlaceOrderCommand(created.orderId()));
        assertThat(afterSecondWrite.version()).isEqualTo(2L);
    }

    @Test
    @DisplayName("a complete purchase runs draft -> placed -> paid -> shipped, publishing each decision")
    void fullHappyPath() {
        UUID orderId = createDraftOrder.createDraftOrder(createCommand()).orderId();
        addOrderLine.addOrderLine(lineCommand(orderId, "Mechanical keyboard", "120.00", 2));

        OrderView placed = placeOrder.placeOrder(new PlaceOrderCommand(orderId));
        assertThat(placed.status()).isEqualTo("PLACED");
        assertThat(placed.subtotal()).isEqualByComparingTo("240.00");
        assertThat(placed.discount()).isEqualByComparingTo("24.00");
        assertThat(placed.total()).isEqualByComparingTo("216.00");
        assertThat(placed.placedAt()).isEqualTo(NOW);

        OrderView paid = payOrder.payOrder(new PayOrderCommand(orderId, "stripe-ch-1"));
        assertThat(paid.status()).isEqualTo("PAID");

        OrderView shipped = shipOrder.shipOrder(new ShipOrderCommand(orderId, "TRACK-99"));
        assertThat(shipped.status()).isEqualTo("SHIPPED");
        assertThat(shipped.trackingNumber()).isEqualTo("TRACK-99");

        assertThat(events.publishedOfType(OrderPlaced.class)).singleElement()
                .satisfies(event -> assertThat(event.total()).isEqualTo(Money.of("216.00", "EUR")));
        assertThat(events.publishedOfType(OrderPaid.class)).singleElement()
                .satisfies(event -> assertThat(event.amountPaid()).isEqualTo(Money.of("216.00", "EUR")));
    }

    @Test
    @DisplayName("editing lines reprices the order without publishing anything")
    void lineEditsAreNotBusinessEvents() {
        UUID orderId = createDraftOrder.createDraftOrder(createCommand()).orderId();
        OrderView withLine = addOrderLine.addOrderLine(lineCommand(orderId, "Trackball", "45.00", 1));
        UUID lineId = withLine.lines().get(0).lineId();

        OrderView increased = changeQuantity.changeOrderLineQuantity(
                new ChangeOrderLineQuantityCommand(orderId, lineId, 3));
        assertThat(increased.total()).isEqualByComparingTo("135.00");

        OrderView emptied = removeOrderLine.removeOrderLine(new RemoveOrderLineCommand(orderId, lineId));
        assertThat(emptied.lines()).isEmpty();
        assertThat(emptied.total()).isEqualByComparingTo("0.00");

        assertThat(events.published()).isEmpty();
    }

    @Test
    @DisplayName("cancelling publishes the status it was cancelled from")
    void cancellingPublishesOrigin() {
        UUID orderId = placedOrder();

        OrderView cancelled = cancelOrder.cancelOrder(new CancelOrderCommand(orderId, "out of stock"));

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.cancellationReason()).isEqualTo("out of stock");
        assertThat(cancelled.cancelledAt()).isEqualTo(NOW);
        assertThat(events.publishedOfType(OrderCancelled.class)).singleElement()
                .satisfies(event -> assertThat(event.cancelledFrom().name()).isEqualTo("PLACED"));
    }

    @Test
    @DisplayName("a command against an unknown order is reported as not found")
    void unknownOrderIsReported() {
        assertThatThrownBy(() -> placeOrder.placeOrder(new PlaceOrderCommand(UUID.randomUUID())))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("a structurally broken command is rejected before any aggregate is loaded")
    void malformedCommandsAreRejectedEarly() {
        assertThatThrownBy(() -> placeOrder.placeOrder(new PlaceOrderCommand(null)))
                .isInstanceOf(InvalidCommandException.class);
        assertThatThrownBy(() -> createDraftOrder.createDraftOrder(
                new CreateDraftOrderCommand(CUSTOMER, "XYZ", "Kerkstraat 1", "Amsterdam", "1012 AB", "NL")))
                .isInstanceOf(InvalidCommandException.class)
                .hasMessageContaining("unknown currency");
        assertThatThrownBy(() -> payOrder.payOrder(new PayOrderCommand(UUID.randomUUID(), " ")))
                .isInstanceOf(InvalidCommandException.class);
        assertThat(orders.size()).isZero();
    }

    @Test
    @DisplayName("a domain rule broken mid-command leaves nothing published and nothing saved")
    void domainFailuresPublishNothing() {
        UUID orderId = placedOrder();
        events.clear();

        assertThatThrownBy(() -> shipOrder.shipOrder(new ShipOrderCommand(orderId, "TRACK-1")))
                .isInstanceOf(InvalidOrderStateException.class);

        assertThat(events.published()).isEmpty();
    }

    @Test
    @DisplayName("an invalid quantity never reaches the aggregate")
    void invalidQuantityIsRejected() {
        UUID orderId = createDraftOrder.createDraftOrder(createCommand()).orderId();

        assertThatThrownBy(() -> addOrderLine.addOrderLine(lineCommand(orderId, "Widget", "10.00", 0)))
                .isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    @DisplayName("a command reads inside its own transaction, so a concurrent write cannot be missed")
    void commandsReadInsideTheirTransaction() {
        UUID orderId = createDraftOrder.createDraftOrder(createCommand()).orderId();
        addOrderLine.addOrderLine(lineCommand(orderId, "Widget", "10.00", 1));

        // A competing writer commits before this command runs. Because the use case loads the
        // aggregate inside its own transaction, it simply sees the newer state and proceeds.
        orders.bumpVersionOf(OrderId.of(orderId));

        OrderView placed = placeOrder.placeOrder(new PlaceOrderCommand(orderId));

        assertThat(placed.status()).isEqualTo("PLACED");
    }

    @Test
    @DisplayName("saving an aggregate held across transactions is refused rather than overwriting")
    void staleWritesAreRefused() {
        UUID orderId = createDraftOrder.createDraftOrder(createCommand()).orderId();
        // An aggregate read now and written later — the case the version check exists for.
        Order heldAggregate = orders.findById(OrderId.of(orderId)).orElseThrow();

        orders.bumpVersionOf(OrderId.of(orderId));
        heldAggregate.addLine(ProductId.of(UUID.randomUUID()), "Widget", Money.of("10.00", "EUR"), Quantity.of(1));

        assertThatThrownBy(() -> orders.save(heldAggregate))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    @DisplayName("each command runs in exactly one transaction")
    void oneTransactionPerCommand() {
        UUID orderId = createDraftOrder.createDraftOrder(createCommand()).orderId();
        addOrderLine.addOrderLine(lineCommand(orderId, "Widget", "10.00", 1));
        placeOrder.placeOrder(new PlaceOrderCommand(orderId));

        assertThat(transactions.invocations()).isEqualTo(3);
    }

    private UUID placedOrder() {
        UUID orderId = createDraftOrder.createDraftOrder(createCommand()).orderId();
        addOrderLine.addOrderLine(lineCommand(orderId, "Mechanical keyboard", "120.00", 1));
        placeOrder.placeOrder(new PlaceOrderCommand(orderId));
        return orderId;
    }

    private static CreateDraftOrderCommand createCommand() {
        return new CreateDraftOrderCommand(CUSTOMER, "EUR", "Kerkstraat 1", "Amsterdam", "1012 AB", "NL");
    }

    private static AddOrderLineCommand lineCommand(UUID orderId, String name, String price, int quantity) {
        return new AddOrderLineCommand(orderId, UUID.randomUUID(), name, new BigDecimal(price), "EUR", quantity);
    }
}
