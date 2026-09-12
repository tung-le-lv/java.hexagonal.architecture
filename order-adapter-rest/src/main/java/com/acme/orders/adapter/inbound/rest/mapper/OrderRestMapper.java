package com.acme.orders.adapter.inbound.rest.mapper;

import com.acme.orders.adapter.inbound.rest.dto.AddOrderLineRequest;
import com.acme.orders.adapter.inbound.rest.dto.AddressPayload;
import com.acme.orders.adapter.inbound.rest.dto.CancelOrderRequest;
import com.acme.orders.adapter.inbound.rest.dto.ChangeQuantityRequest;
import com.acme.orders.adapter.inbound.rest.dto.CreateOrderRequest;
import com.acme.orders.adapter.inbound.rest.dto.OrderResponse;
import com.acme.orders.adapter.inbound.rest.dto.OrderSummaryResponse;
import com.acme.orders.adapter.inbound.rest.dto.PagedResponse;
import com.acme.orders.adapter.inbound.rest.dto.PayOrderRequest;
import com.acme.orders.adapter.inbound.rest.dto.ShipOrderRequest;
import com.acme.orders.application.port.inbound.command.AddOrderLineCommand;
import com.acme.orders.application.port.inbound.command.CancelOrderCommand;
import com.acme.orders.application.port.inbound.command.ChangeOrderLineQuantityCommand;
import com.acme.orders.application.port.inbound.command.CreateDraftOrderCommand;
import com.acme.orders.application.port.inbound.command.PayOrderCommand;
import com.acme.orders.application.port.inbound.command.ShipOrderCommand;
import com.acme.orders.application.view.OrderSummaryView;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.application.view.Page;
import java.util.List;
import java.util.UUID;

/**
 * The translation layer between HTTP and the application's ports.
 *
 * <p>Its existence is the point: because every request becomes a command and every view becomes a
 * response here, the JSON contract and the application's vocabulary can evolve independently, and a
 * second inbound adapter (gRPC, a CLI, a message consumer) needs nothing from this class.
 */
public final class OrderRestMapper {

    private OrderRestMapper() {
    }

    // ------------------------------------------------------------------ request -> command

    public static CreateDraftOrderCommand toCommand(CreateOrderRequest request) {
        AddressPayload address = request.shippingAddress();
        return new CreateDraftOrderCommand(
                request.customerId(),
                request.currency(),
                address.street(),
                address.city(),
                address.postalCode(),
                address.countryCode());
    }

    public static AddOrderLineCommand toCommand(UUID orderId, AddOrderLineRequest request) {
        return new AddOrderLineCommand(
                orderId,
                request.productId(),
                request.productName(),
                request.unitPrice(),
                request.currency(),
                request.quantity());
    }

    public static ChangeOrderLineQuantityCommand toCommand(UUID orderId, UUID lineId, ChangeQuantityRequest request) {
        return new ChangeOrderLineQuantityCommand(orderId, lineId, request.quantity());
    }

    public static PayOrderCommand toCommand(UUID orderId, PayOrderRequest request) {
        return new PayOrderCommand(orderId, request.paymentReference());
    }

    public static ShipOrderCommand toCommand(UUID orderId, ShipOrderRequest request) {
        return new ShipOrderCommand(orderId, request.trackingNumber());
    }

    public static CancelOrderCommand toCommand(UUID orderId, CancelOrderRequest request) {
        return new CancelOrderCommand(orderId, request.reason());
    }

    // ------------------------------------------------------------------ view -> response

    public static OrderResponse toResponse(OrderView view) {
        return new OrderResponse(
                view.orderId(),
                view.customerId(),
                view.status(),
                view.currency(),
                new AddressPayload(
                        view.shippingAddress().street(),
                        view.shippingAddress().city(),
                        view.shippingAddress().postalCode(),
                        view.shippingAddress().countryCode()),
                view.lines().stream().map(OrderRestMapper::toPayload).toList(),
                view.subtotal(),
                view.discount(),
                view.total(),
                view.totalItemCount(),
                view.createdAt(),
                view.placedAt(),
                view.paidAt(),
                view.shippedAt(),
                view.cancelledAt(),
                view.cancellationReason(),
                view.paymentReference(),
                view.trackingNumber(),
                view.version());
    }

    public static PagedResponse<OrderSummaryResponse> toResponse(Page<OrderSummaryView> page) {
        List<OrderSummaryResponse> items = page.content().stream()
                .map(OrderRestMapper::toResponse)
                .toList();
        return new PagedResponse<>(items, page.pageNumber(), page.pageSize(), page.totalElements(),
                page.totalPages(), page.hasNext());
    }

    private static OrderSummaryResponse toResponse(OrderSummaryView view) {
        return new OrderSummaryResponse(
                view.orderId(),
                view.customerId(),
                view.status(),
                view.currency(),
                view.total(),
                view.lineCount(),
                view.createdAt(),
                view.placedAt());
    }

    private static OrderResponse.OrderLinePayload toPayload(OrderView.OrderLineView line) {
        return new OrderResponse.OrderLinePayload(
                line.lineId(),
                line.productId(),
                line.productName(),
                line.quantity(),
                line.unitPrice(),
                line.lineTotal());
    }
}
