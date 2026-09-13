package com.acme.orders.application.port.outbound;

import com.acme.orders.application.view.OrderSummaryView;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.application.view.Page;
import com.acme.orders.domain.repository.IOrderRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for the read side: returns projections, never aggregates.
 *
 * <p>Splitting this from {@link IOrderRepository} lets the adapter answer queries however it is
 * fastest — a join, a view, a denormalised table — without that choice reaching into the model.
 * Unlike {@code IOrderRepository}, this port stays here rather than moving to the domain: it
 * returns application-level view types, not the aggregate, so it isn't a DDD repository — it's a
 * CQRS read port the application defines for its own querying needs.
 */
public interface IOrderQueryRepository {

    Optional<OrderView> findById(UUID orderId);

    Page<OrderSummaryView> findByCustomerId(UUID customerId, int pageNumber, int pageSize);
}
