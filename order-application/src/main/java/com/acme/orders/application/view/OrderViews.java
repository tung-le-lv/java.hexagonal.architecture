package com.acme.orders.application.view;

import com.acme.orders.domain.model.order.Order;
import com.acme.orders.domain.model.order.OrderLine;
import com.acme.orders.domain.model.shared.Address;

/**
 * Turns an {@link Order} aggregate into its read model.
 *
 * <p>Lives in the application layer, not the domain: the aggregate should not know that anyone wants
 * a flat, serialisable copy of it.
 */
public final class OrderViews {

    private OrderViews() {
    }

    public static OrderView from(Order order) {
        return new OrderView(
                order.id().value(),
                order.customerId().value(),
                order.status().name(),
                order.currency().getCurrencyCode(),
                from(order.shippingAddress()),
                order.lines().stream().map(OrderViews::from).toList(),
                order.subtotal().amount(),
                order.discount().amount(),
                order.total().amount(),
                order.totalItemCount(),
                order.createdAt(),
                order.placedAt().orElse(null),
                order.paidAt().orElse(null),
                order.shippedAt().orElse(null),
                order.cancelledAt().orElse(null),
                order.cancellationReason().orElse(null),
                order.paymentReference().orElse(null),
                order.trackingNumber().orElse(null),
                order.version());
    }

    public static OrderSummaryView summaryFrom(Order order) {
        return new OrderSummaryView(
                order.id().value(),
                order.customerId().value(),
                order.status().name(),
                order.currency().getCurrencyCode(),
                order.total().amount(),
                order.lines().size(),
                order.createdAt(),
                order.placedAt().orElse(null));
    }

    private static OrderView.OrderLineView from(OrderLine line) {
        return new OrderView.OrderLineView(
                line.id().value(),
                line.productId().value(),
                line.productName(),
                line.quantity().value(),
                line.unitPrice().amount(),
                line.lineTotal().amount());
    }

    private static OrderView.AddressView from(Address address) {
        return new OrderView.AddressView(address.street(), address.city(), address.postalCode(), address.countryCode());
    }
}
