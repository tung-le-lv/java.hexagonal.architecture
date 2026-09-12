package com.acme.orders.domain.aggregate;

import com.acme.orders.domain.event.OrderCancelled;
import com.acme.orders.domain.event.OrderDelivered;
import com.acme.orders.domain.event.OrderPaid;
import com.acme.orders.domain.event.OrderPlaced;
import com.acme.orders.domain.event.OrderShipped;
import com.acme.orders.domain.exception.CurrencyMismatchException;
import com.acme.orders.domain.exception.EmptyOrderException;
import com.acme.orders.domain.exception.InvalidOrderStateException;
import com.acme.orders.domain.exception.MissingOrderDetailException;
import com.acme.orders.domain.exception.OrderLimitExceededException;
import com.acme.orders.domain.exception.OrderLineNotFoundException;
import com.acme.orders.domain.service.IDiscountPolicy;
import com.acme.orders.domain.valueobject.Address;
import com.acme.orders.domain.valueobject.CustomerId;
import com.acme.orders.domain.valueobject.Money;
import com.acme.orders.domain.valueobject.OrderId;
import com.acme.orders.domain.valueobject.OrderLineId;
import com.acme.orders.domain.valueobject.OrderSnapshot;
import com.acme.orders.domain.valueobject.OrderStatus;
import com.acme.orders.domain.valueobject.ProductId;
import com.acme.orders.domain.valueobject.Quantity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An order: the consistency boundary for everything a customer is buying in one purchase.
 *
 * <p>The aggregate owns its rules instead of exposing its state for someone else to check. There
 * are no setters and no public constructor; every transition is a named business operation that
 * either completes and records what happened, or throws. In particular:
 *
 * <ul>
 *   <li>lines may be edited only while the order is {@link OrderStatus#DRAFT};
 *   <li>every line's price must be in the order's currency, so the total is always meaningful;
 *   <li>an order with no lines cannot be placed;
 *   <li>status changes follow {@link OrderStatus}'s transition table and nothing else;
 *   <li>the total is derived from the lines on every read, so it can never disagree with them.
 * </ul>
 *
 * <p>Timestamps are supplied by the caller rather than read from a clock: placement time is a
 * business fact, and taking it as a parameter keeps the core free of ambient dependencies and the
 * tests free of sleeps.
 */
public class Order extends AggregateRoot<OrderId> {

    /** A single order is a basket, not a catalogue; beyond this the customer wants a bulk channel. */
    private static final int MAX_LINES = 50;

    private final OrderId id;
    private final CustomerId customerId;
    private final Currency currency;
    private final List<OrderLine> lines;
    private final Instant createdAt;

    private OrderStatus status;
    private Address shippingAddress;
    private Money discount;
    private Instant placedAt;
    private Instant paidAt;
    private Instant shippedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private String paymentReference;
    private String trackingNumber;
    private final long version;

    private Order(OrderSnapshot snapshot) {
        this.id = Objects.requireNonNull(snapshot.id(), "order id must not be null");
        this.customerId = Objects.requireNonNull(snapshot.customerId(), "customer id must not be null");
        this.currency = Objects.requireNonNull(snapshot.currency(), "currency must not be null");
        this.status = Objects.requireNonNull(snapshot.status(), "status must not be null");
        this.shippingAddress = Objects.requireNonNull(snapshot.shippingAddress(), "shipping address must not be null");
        this.lines = new ArrayList<>(snapshot.lines());
        this.discount = snapshot.discount() == null ? Money.zero(snapshot.currency()) : snapshot.discount();
        this.createdAt = Objects.requireNonNull(snapshot.createdAt(), "createdAt must not be null");
        this.placedAt = snapshot.placedAt();
        this.paidAt = snapshot.paidAt();
        this.shippedAt = snapshot.shippedAt();
        this.cancelledAt = snapshot.cancelledAt();
        this.cancellationReason = snapshot.cancellationReason();
        this.paymentReference = snapshot.paymentReference();
        this.trackingNumber = snapshot.trackingNumber();
        this.version = snapshot.version();
    }

    /** Starts a new, empty order in {@link OrderStatus#DRAFT}. */
    public static Order draft(CustomerId customerId, Currency currency, Address shippingAddress, Instant createdAt) {
        return new Order(new OrderSnapshot(
                OrderId.newId(), customerId, OrderStatus.DRAFT, currency, shippingAddress,
                List.of(), Money.zero(currency), createdAt,
                null, null, null, null, null, null, null, 0L));
    }

    /**
     * Rebuilds an order from stored state, bypassing the lifecycle rules that governed how it got
     * there. For outbound persistence adapters only — it raises no events, because reading history
     * back is not a new business decision.
     */
    public static Order rehydrate(OrderSnapshot snapshot) {
        return new Order(snapshot);
    }

    /** Externalises the full state for persistence, without opening the model up to mutation. */
    public OrderSnapshot toSnapshot() {
        return new OrderSnapshot(id, customerId, status, currency, shippingAddress, List.copyOf(lines), discount,
                createdAt, placedAt, paidAt, shippedAt, cancelledAt, cancellationReason, paymentReference,
                trackingNumber, version);
    }

    // ---------------------------------------------------------------- draft editing

    /**
     * Adds a product to the draft, or increases the quantity if the product is already on it.
     *
     * <p>Merging rather than duplicating keeps one line per product, which is what makes
     * "change the quantity of X" an unambiguous command later.
     *
     * @return the id of the line that now carries this product
     */
    public OrderLineId addLine(ProductId productId, String productName, Money unitPrice, Quantity quantity) {
        requireEditable("add a line to");
        requireOrderCurrency(unitPrice);

        Optional<OrderLine> existing = findLineFor(productId);
        if (existing.isPresent()) {
            OrderLine line = existing.get();
            line.increaseQuantityBy(quantity);
            return line.id();
        }

        if (lines.size() >= MAX_LINES) {
            throw new OrderLimitExceededException("an order must not have more than " + MAX_LINES + " lines");
        }
        OrderLine line = OrderLine.create(productId, productName, unitPrice, quantity);
        lines.add(line);
        return line.id();
    }

    public void changeLineQuantity(OrderLineId lineId, Quantity newQuantity) {
        requireEditable("change a line of");
        requireLine(lineId).changeQuantity(newQuantity);
    }

    public void removeLine(OrderLineId lineId) {
        requireEditable("remove a line from");
        lines.remove(requireLine(lineId));
    }

    public void changeShippingAddress(Address newAddress) {
        requireEditable("change the shipping address of");
        this.shippingAddress = Objects.requireNonNull(newAddress, "shipping address must not be null");
    }

    // ---------------------------------------------------------------- lifecycle

    /**
     * Submits the order: prices it with the policy in force and freezes the result.
     *
     * <p>The discount is taken as a collaborator rather than computed here, so pricing rules can
     * change without touching the aggregate. Whatever the policy returns is still validated — the
     * order, not the policy, is responsible for never ending up with a nonsensical total.
     */
    public void place(IDiscountPolicy discountPolicy, Instant placedAt) {
        Objects.requireNonNull(discountPolicy, "discount policy must not be null");
        requireTransitionTo(OrderStatus.PLACED, "place");
        if (lines.isEmpty()) {
            throw new EmptyOrderException(id.toString());
        }

        this.discount = validated(discountPolicy.discountFor(this));
        this.status = OrderStatus.PLACED;
        this.placedAt = Objects.requireNonNull(placedAt, "placedAt must not be null");

        raise(OrderPlaced.of(id.value(), customerId.value(), subtotal(), discount, total(),
                lines.stream()
                        .map(line -> new OrderPlaced.PlacedLine(line.productId().value(), line.productName(),
                                line.quantity().value(), line.unitPrice(), line.lineTotal()))
                        .toList(),
                placedAt));
    }

    public void pay(String paymentReference, Instant paidAt) {
        requireTransitionTo(OrderStatus.PAID, "pay for");
        this.paymentReference = requireText(paymentReference, "payment reference");
        this.status = OrderStatus.PAID;
        this.paidAt = Objects.requireNonNull(paidAt, "paidAt must not be null");

        raise(OrderPaid.of(id.value(), customerId.value(), total(), this.paymentReference, paidAt));
    }

    public void ship(String trackingNumber, Instant shippedAt) {
        requireTransitionTo(OrderStatus.SHIPPED, "ship");
        this.trackingNumber = requireText(trackingNumber, "tracking number");
        this.status = OrderStatus.SHIPPED;
        this.shippedAt = Objects.requireNonNull(shippedAt, "shippedAt must not be null");

        raise(OrderShipped.of(id.value(), customerId.value(), this.trackingNumber, shippingAddress, shippedAt));
    }

    public void markDelivered(Instant deliveredAt) {
        requireTransitionTo(OrderStatus.DELIVERED, "mark as delivered");
        this.status = OrderStatus.DELIVERED;

        raise(OrderDelivered.of(id.value(), customerId.value(),
                Objects.requireNonNull(deliveredAt, "deliveredAt must not be null")));
    }

    /** Abandons the order. Allowed until it has shipped, after which returns are a different process. */
    public void cancel(String reason, Instant cancelledAt) {
        requireTransitionTo(OrderStatus.CANCELLED, "cancel");
        OrderStatus cancelledFrom = this.status;
        this.cancellationReason = requireText(reason, "cancellation reason");
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");

        raise(OrderCancelled.of(id.value(), customerId.value(), cancelledFrom, this.cancellationReason, cancelledAt));
    }

    // ---------------------------------------------------------------- derived state

    /** Sum of the lines before discount. Derived on every call so it cannot drift from the lines. */
    public Money subtotal() {
        return lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(Money.zero(currency), Money::add);
    }

    public Money total() {
        return subtotal().subtract(discount);
    }

    public int totalItemCount() {
        return lines.stream().mapToInt(line -> line.quantity().value()).sum();
    }

    // ---------------------------------------------------------------- accessors

    @Override
    public OrderId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public OrderStatus status() {
        return status;
    }

    public Currency currency() {
        return currency;
    }

    public Address shippingAddress() {
        return shippingAddress;
    }

    /** Read-only view: lines are changed through the root, never through this list. */
    public List<OrderLine> lines() {
        return Collections.unmodifiableList(lines);
    }

    public Money discount() {
        return discount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> placedAt() {
        return Optional.ofNullable(placedAt);
    }

    public Optional<Instant> paidAt() {
        return Optional.ofNullable(paidAt);
    }

    public Optional<Instant> shippedAt() {
        return Optional.ofNullable(shippedAt);
    }

    public Optional<Instant> cancelledAt() {
        return Optional.ofNullable(cancelledAt);
    }

    public Optional<String> cancellationReason() {
        return Optional.ofNullable(cancellationReason);
    }

    public Optional<String> paymentReference() {
        return Optional.ofNullable(paymentReference);
    }

    public Optional<String> trackingNumber() {
        return Optional.ofNullable(trackingNumber);
    }

    public long version() {
        return version;
    }

    // ---------------------------------------------------------------- guards

    private void requireEditable(String operation) {
        if (!status.isEditable()) {
            throw new InvalidOrderStateException(operation, status, OrderStatus.DRAFT);
        }
    }

    private void requireTransitionTo(OrderStatus target, String operation) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidOrderStateException(operation, status,
                    status.allowedTransitions().toArray(OrderStatus[]::new));
        }
    }

    private void requireOrderCurrency(Money amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (!amount.currency().equals(currency)) {
            throw new CurrencyMismatchException(currency.getCurrencyCode(), amount.currency().getCurrencyCode());
        }
    }

    private Money validated(Money proposedDiscount) {
        Objects.requireNonNull(proposedDiscount, "discount must not be null");
        requireOrderCurrency(proposedDiscount);
        if (proposedDiscount.isNegative()) {
            throw new OrderLimitExceededException("discount must not be negative but was " + proposedDiscount);
        }
        if (proposedDiscount.isGreaterThan(subtotal())) {
            throw new OrderLimitExceededException(
                    "discount " + proposedDiscount + " must not exceed the subtotal " + subtotal());
        }
        return proposedDiscount;
    }

    private OrderLine requireLine(OrderLineId lineId) {
        Objects.requireNonNull(lineId, "line id must not be null");
        return lines.stream()
                .filter(line -> line.id().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new OrderLineNotFoundException(id.toString(), lineId.toString()));
    }

    private Optional<OrderLine> findLineFor(ProductId productId) {
        Objects.requireNonNull(productId, "product id must not be null");
        return lines.stream().filter(line -> line.isFor(productId)).findFirst();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new MissingOrderDetailException(field + " must not be blank");
        }
        return value.strip();
    }

    @Override
    public String toString() {
        return "Order[" + id + ", " + status + ", " + lines.size() + " lines, total " + total() + "]";
    }
}
