package com.acme.orders.application.usecase;

import com.acme.orders.application.exception.InvalidCommandException;
import com.acme.orders.application.exception.OrderNotFoundException;
import com.acme.orders.application.port.inbound.query.GetOrderQuery;
import com.acme.orders.application.port.inbound.query.ListCustomerOrdersQuery;
import com.acme.orders.application.port.outbound.OrderQueryRepository;
import com.acme.orders.application.view.OrderSummaryView;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.application.view.Page;
import java.util.Objects;
import java.util.UUID;

/**
 * The read side. No aggregate is loaded and no transaction is opened: a query changes nothing, so
 * paying for the write machinery would buy nothing.
 */
public class OrderQueryService implements GetOrderQuery, ListCustomerOrdersQuery {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderQueryRepository orderQueries;

    public OrderQueryService(OrderQueryRepository orderQueries) {
        this.orderQueries = Objects.requireNonNull(orderQueries, "orderQueries must not be null");
    }

    @Override
    public OrderView byId(UUID orderId) {
        UUID id = Commands.requireId(orderId, "orderId");
        return orderQueries.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    public Page<OrderSummaryView> forCustomer(UUID customerId, int pageNumber, int pageSize) {
        UUID id = Commands.requireId(customerId, "customerId");
        if (pageNumber < 0) {
            throw new InvalidCommandException("pageNumber must not be negative but was " + pageNumber);
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new InvalidCommandException("pageSize must be between 1 and " + MAX_PAGE_SIZE + " but was " + pageSize);
        }
        return orderQueries.findByCustomerId(id, pageNumber, pageSize);
    }
}
