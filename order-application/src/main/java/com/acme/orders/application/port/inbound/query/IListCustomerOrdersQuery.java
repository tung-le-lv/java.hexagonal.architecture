package com.acme.orders.application.port.inbound.query;

import com.acme.orders.application.view.OrderSummaryView;
import com.acme.orders.application.view.Page;
import java.util.UUID;

/** Driving port for reads: a customer's orders, most recent first. */
public interface IListCustomerOrdersQuery {

    Page<OrderSummaryView> forCustomer(UUID customerId, int pageNumber, int pageSize);
}
