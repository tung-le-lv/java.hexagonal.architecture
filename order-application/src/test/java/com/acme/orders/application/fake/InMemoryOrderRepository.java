package com.acme.orders.application.fake;

import com.acme.orders.application.exception.ConcurrentModificationException;
import com.acme.orders.application.port.outbound.IOrderRepository;
import com.acme.orders.domain.model.order.Order;
import com.acme.orders.domain.model.order.OrderId;
import com.acme.orders.domain.model.order.OrderSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A working repository in twenty lines, because the port asks for nothing a database is needed for.
 *
 * <p>That this is possible at all is the payoff of the dependency inversion: the use case tests below
 * run in milliseconds with no schema, no container and no transaction manager, and still exercise the
 * real production code path — including version checking, which it reproduces faithfully enough to
 * test the concurrency behaviour.
 */
public class InMemoryOrderRepository implements IOrderRepository {

    private final Map<OrderId, OrderSnapshot> stored = new HashMap<>();

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return Optional.ofNullable(stored.get(orderId)).map(Order::rehydrate);
    }

    @Override
    public Order save(Order order) {
        OrderSnapshot incoming = order.toSnapshot();
        OrderSnapshot existing = stored.get(order.id());

        if (existing == null) {
            // Matches Hibernate's @Version: a fresh insert is version 0, not 1.
            OrderSnapshot inserted = incoming.withVersion(0L);
            stored.put(order.id(), inserted);
            return Order.rehydrate(inserted);
        }

        if (existing.version() != incoming.version()) {
            throw new ConcurrentModificationException(order.id().value(), null);
        }
        OrderSnapshot updated = incoming.withVersion(incoming.version() + 1);
        stored.put(order.id(), updated);
        return Order.rehydrate(updated);
    }

    @Override
    public boolean existsById(OrderId orderId) {
        return stored.containsKey(orderId);
    }

    /** Simulates a competing writer having advanced the stored version. */
    public void bumpVersionOf(OrderId orderId) {
        OrderSnapshot snapshot = stored.get(orderId);
        stored.put(orderId, snapshot.withVersion(snapshot.version() + 1));
    }

    public int size() {
        return stored.size();
    }
}
