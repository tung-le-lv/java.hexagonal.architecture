package com.acme.orders.application.port.outbound;

import com.acme.orders.application.view.OrderSummaryView;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.application.view.Page;
import java.util.Optional;
import java.util.UUID;

/**
 * Driven port for the read side: returns projections, never aggregates.
 *
 * <p>Splitting this from {@link IOrderRepository} lets the adapter answer queries however it is
 * fastest — a join, a view, a denormalised table — without that choice reaching into the model.
 */
public interface IOrderQueryRepository {

    Optional<OrderView> findById(UUID orderId);

    Page<OrderSummaryView> findByCustomerId(UUID customerId, int pageNumber, int pageSize);
}
