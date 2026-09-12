package com.acme.orders.domain.aggregate;

import com.acme.orders.domain.exception.InvalidOrderLineException;
import com.acme.orders.domain.valueobject.Money;
import com.acme.orders.domain.valueobject.OrderLineId;
import com.acme.orders.domain.valueobject.ProductId;
import com.acme.orders.domain.valueobject.Quantity;
import java.util.Objects;

/**
 * A product, its agreed unit price, and how many of it the customer wants.
 *
 * <p>An entity rather than a value object: its quantity changes over the draft's life while its
 * identity stays the same. It lives strictly inside the {@link Order} aggregate — mutators are
 * package-private, so the only way to change a line is to go through the root, which is what keeps
 * the order's invariants enforceable.
 */
public class OrderLine {

    private final OrderLineId id;
    private final ProductId productId;
    private final String productName;
    private final Money unitPrice;
    private Quantity quantity;

    private OrderLine(OrderLineId id, ProductId productId, String productName, Money unitPrice, Quantity quantity) {
        this.id = Objects.requireNonNull(id, "line id must not be null");
        this.productId = Objects.requireNonNull(productId, "product id must not be null");
        this.productName = requireName(productName);
        this.unitPrice = requireSellablePrice(unitPrice);
        this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
    }

    static OrderLine create(ProductId productId, String productName, Money unitPrice, Quantity quantity) {
        return new OrderLine(OrderLineId.newId(), productId, productName, unitPrice, quantity);
    }

    /**
     * Rebuilds a line from stored state.
     *
     * <p>For outbound persistence adapters only: it skips nothing, but it also raises no events,
     * because replaying history is not a business decision.
     */
    public static OrderLine rehydrate(OrderLineId id, ProductId productId, String productName, Money unitPrice, Quantity quantity) {
        return new OrderLine(id, productId, productName, unitPrice, quantity);
    }

    /** What this line costs: unit price times quantity. Always derived, never stored. */
    public Money lineTotal() {
        return unitPrice.multiply(quantity.value());
    }

    void changeQuantity(Quantity newQuantity) {
        this.quantity = Objects.requireNonNull(newQuantity, "quantity must not be null");
    }

    void increaseQuantityBy(Quantity additional) {
        this.quantity = this.quantity.plus(additional);
    }

    boolean isFor(ProductId candidate) {
        return productId.equals(candidate);
    }

    public OrderLineId id() {
        return id;
    }

    public ProductId productId() {
        return productId;
    }

    public String productName() {
        return productName;
    }

    public Money unitPrice() {
        return unitPrice;
    }

    public Quantity quantity() {
        return quantity;
    }

    private static String requireName(String productName) {
        if (productName == null || productName.isBlank()) {
            throw new InvalidOrderLineException("product name must not be blank");
        }
        return productName.strip();
    }

    private static Money requireSellablePrice(Money unitPrice) {
        Objects.requireNonNull(unitPrice, "unit price must not be null");
        if (unitPrice.isNegative()) {
            throw new InvalidOrderLineException("unit price must not be negative but was " + unitPrice);
        }
        return unitPrice;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof OrderLine line && id.equals(line.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return quantity + " x " + productName + " @ " + unitPrice;
    }
}
