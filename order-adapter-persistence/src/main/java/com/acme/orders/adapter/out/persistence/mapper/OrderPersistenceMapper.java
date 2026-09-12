package com.acme.orders.adapter.out.persistence.mapper;

import com.acme.orders.adapter.out.persistence.entity.OrderJpaEntity;
import com.acme.orders.adapter.out.persistence.entity.OrderLineJpaEntity;
import com.acme.orders.application.view.OrderSummaryView;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.domain.model.order.CustomerId;
import com.acme.orders.domain.model.order.Order;
import com.acme.orders.domain.model.order.OrderId;
import com.acme.orders.domain.model.order.OrderLine;
import com.acme.orders.domain.model.order.OrderLineId;
import com.acme.orders.domain.model.order.OrderSnapshot;
import com.acme.orders.domain.model.order.OrderStatus;
import com.acme.orders.domain.model.order.ProductId;
import com.acme.orders.domain.model.shared.Address;
import com.acme.orders.domain.model.shared.Money;
import com.acme.orders.domain.model.shared.Quantity;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Translates between the aggregate and its rows, in both directions.
 *
 * <p>All the tedium of the hexagon's outbound edge is concentrated here on purpose. The cost is this
 * class; the benefit is that the model owes nothing to JPA and the schema owes nothing to the model,
 * so either can be changed without touching the other.
 */
public final class OrderPersistenceMapper {

    private OrderPersistenceMapper() {
    }

    // ------------------------------------------------------------------ domain -> rows

    /** Builds a row for an order that has never been persisted. */
    public static OrderJpaEntity toNewEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        copyScalars(order, entity);
        order.toSnapshot().lines().forEach(line -> entity.getLines().add(toEntity(line)));
        return entity;
    }

    /**
     * Copies the aggregate's state onto a row that is already managed by the persistence context.
     *
     * <p>Lines are reconciled by identity rather than cleared and re-added: clearing would make
     * Hibernate delete and re-insert rows that still carry the same primary key, which can fail
     * outright depending on flush order. Matching on id means an unchanged line produces no SQL, a
     * changed quantity produces an UPDATE, and only genuinely removed lines are deleted.
     */
    public static void updateEntity(OrderJpaEntity entity, Order order) {
        copyScalars(order, entity);

        List<OrderLine> lines = order.toSnapshot().lines();
        Map<UUID, OrderLineJpaEntity> existing = entity.getLines().stream()
                .collect(Collectors.toMap(OrderLineJpaEntity::getId, line -> line, (a, b) -> a, LinkedHashMap::new));
        Set<UUID> retained = lines.stream().map(line -> line.id().value()).collect(Collectors.toSet());

        entity.getLines().removeIf(line -> !retained.contains(line.getId()));
        for (OrderLine line : lines) {
            OrderLineJpaEntity row = existing.get(line.id().value());
            if (row == null) {
                entity.getLines().add(toEntity(line));
            } else {
                applyTo(row, line);
            }
        }
    }

    private static void copyScalars(Order order, OrderJpaEntity entity) {
        OrderSnapshot snapshot = order.toSnapshot();
        entity.setId(snapshot.id().value());
        entity.setCustomerId(snapshot.customerId().value());
        entity.setStatus(snapshot.status().name());
        entity.setCurrency(snapshot.currency().getCurrencyCode());
        entity.setStreet(snapshot.shippingAddress().street());
        entity.setCity(snapshot.shippingAddress().city());
        entity.setPostalCode(snapshot.shippingAddress().postalCode());
        entity.setCountryCode(snapshot.shippingAddress().countryCode());
        entity.setDiscountAmount(snapshot.discount().amount());
        entity.setTotalAmount(order.total().amount());
        entity.setCreatedAt(snapshot.createdAt());
        entity.setPlacedAt(snapshot.placedAt());
        entity.setPaidAt(snapshot.paidAt());
        entity.setShippedAt(snapshot.shippedAt());
        entity.setCancelledAt(snapshot.cancelledAt());
        entity.setCancellationReason(snapshot.cancellationReason());
        entity.setPaymentReference(snapshot.paymentReference());
        entity.setTrackingNumber(snapshot.trackingNumber());
        // version is never copied: the store owns it.
    }

    private static OrderLineJpaEntity toEntity(OrderLine line) {
        OrderLineJpaEntity entity = new OrderLineJpaEntity();
        entity.setId(line.id().value());
        applyTo(entity, line);
        return entity;
    }

    private static void applyTo(OrderLineJpaEntity entity, OrderLine line) {
        entity.setProductId(line.productId().value());
        entity.setProductName(line.productName());
        entity.setUnitPrice(line.unitPrice().amount());
        entity.setQuantity(line.quantity().value());
    }

    // ------------------------------------------------------------------ rows -> domain

    public static Order toDomain(OrderJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrency());
        List<OrderLine> lines = entity.getLines().stream()
                .map(line -> toDomain(line, currency))
                .toList();

        return Order.rehydrate(new OrderSnapshot(
                OrderId.of(entity.getId()),
                CustomerId.of(entity.getCustomerId()),
                OrderStatus.valueOf(entity.getStatus()),
                currency,
                Address.of(entity.getStreet(), entity.getCity(), entity.getPostalCode(), entity.getCountryCode()),
                lines,
                Money.of(entity.getDiscountAmount(), currency),
                entity.getCreatedAt(),
                entity.getPlacedAt(),
                entity.getPaidAt(),
                entity.getShippedAt(),
                entity.getCancelledAt(),
                entity.getCancellationReason(),
                entity.getPaymentReference(),
                entity.getTrackingNumber(),
                entity.versionOrZero()));
    }

    private static OrderLine toDomain(OrderLineJpaEntity entity, Currency currency) {
        return OrderLine.rehydrate(
                OrderLineId.of(entity.getId()),
                ProductId.of(entity.getProductId()),
                entity.getProductName(),
                Money.of(entity.getUnitPrice(), currency),
                Quantity.of(entity.getQuantity()));
    }

    // ------------------------------------------------------------------ rows -> read models

    /** Projects straight from rows to the read model, without going through the aggregate. */
    public static OrderView toView(OrderJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrency());
        List<OrderView.OrderLineView> lines = entity.getLines().stream()
                .map(line -> new OrderView.OrderLineView(
                        line.getId(),
                        line.getProductId(),
                        line.getProductName(),
                        line.getQuantity(),
                        Money.of(line.getUnitPrice(), currency).amount(),
                        Money.of(line.getUnitPrice(), currency).multiply(line.getQuantity()).amount()))
                .toList();

        Money subtotal = lines.stream()
                .map(line -> Money.of(line.lineTotal(), currency))
                .reduce(Money.zero(currency), Money::add);
        Money discount = Money.of(entity.getDiscountAmount(), currency);

        return new OrderView(
                entity.getId(),
                entity.getCustomerId(),
                entity.getStatus(),
                entity.getCurrency(),
                new OrderView.AddressView(entity.getStreet(), entity.getCity(), entity.getPostalCode(),
                        entity.getCountryCode()),
                lines,
                subtotal.amount(),
                discount.amount(),
                subtotal.subtract(discount).amount(),
                entity.getLines().stream().mapToInt(OrderLineJpaEntity::getQuantity).sum(),
                entity.getCreatedAt(),
                entity.getPlacedAt(),
                entity.getPaidAt(),
                entity.getShippedAt(),
                entity.getCancelledAt(),
                entity.getCancellationReason(),
                entity.getPaymentReference(),
                entity.getTrackingNumber(),
                entity.versionOrZero());
    }

    public static OrderSummaryView toSummaryView(OrderJpaEntity entity) {
        return new OrderSummaryView(
                entity.getId(),
                entity.getCustomerId(),
                entity.getStatus(),
                entity.getCurrency(),
                entity.getTotalAmount(),
                entity.getLines().size(),
                entity.getCreatedAt(),
                entity.getPlacedAt());
    }
}
