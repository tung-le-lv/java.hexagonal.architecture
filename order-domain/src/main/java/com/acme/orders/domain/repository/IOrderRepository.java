package com.acme.orders.domain.repository;

import com.acme.orders.domain.aggregate.Order;
import com.acme.orders.domain.valueobject.OrderId;
import java.util.Optional;

/**
 * Repository for the write side: loads and stores whole {@link Order} aggregates.
 *
 * <p>Declared in the domain module's {@code repository} package — DDD scopes a repository
 * one-per-aggregate and treats it as domain vocabulary, not an application concern. It is still
 * implemented out in the persistence adapter, so the core stays compilable and testable with no
 * database in sight; only the contract's home changes, not the direction of the dependency. The
 * interface deliberately offers no query methods, no partial updates and no lazy handles: an
 * aggregate is loaded and saved whole, because that is the unit its invariants hold over.
 */
public interface IOrderRepository {

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
