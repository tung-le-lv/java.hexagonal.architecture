package com.acme.orders.application.port.outbound;

import com.acme.orders.domain.model.order.Order;
import com.acme.orders.domain.model.order.OrderId;
import java.util.Optional;

/**
 * Driven port for the write side: loads and stores whole {@link Order} aggregates.
 *
 * <p>Declared here, in application terms, and implemented out in the persistence adapter — that
 * inversion is what lets the core be compiled and tested with no database in sight. The interface
 * deliberately offers no query methods, no partial updates and no lazy handles: an aggregate is
 * loaded and saved whole, because that is the unit its invariants hold over.
 */
public interface OrderRepository {

    Optional<Order> findById(OrderId orderId);

    /**
     * Persists the aggregate's current state.
     *
     * @return the saved aggregate, carrying the version the store assigned
     * @throws com.acme.orders.application.exception.ConcurrentModificationException if another
     *         transaction changed the same order first
     */
    Order save(Order order);

    boolean existsById(OrderId orderId);
}
