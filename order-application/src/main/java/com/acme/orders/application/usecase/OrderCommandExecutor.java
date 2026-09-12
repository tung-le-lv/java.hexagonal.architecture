package com.acme.orders.application.usecase;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.acme.orders.application.exception.OrderNotFoundException;
import com.acme.orders.application.port.outbound.IDomainEventPublisher;
import com.acme.orders.application.port.outbound.ITransactionRunner;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.application.view.OrderViews;
import com.acme.orders.domain.repository.IOrderRepository;
import com.acme.orders.domain.aggregate.Order;
import com.acme.orders.domain.valueobject.OrderId;

/**
 * The one shape every write use case has: open a transaction, load the aggregate, let it decide,
 * save it, publish what it decided.
 *
 * <p>Factored out so the use cases below contain only the part that differs — which command they
 * hand the aggregate. Package-private: this is internal plumbing, not a port.
 */
final class OrderCommandExecutor {

    private final IOrderRepository orderRepository;
    private final IDomainEventPublisher eventPublisher;
    private final ITransactionRunner transactions;

    OrderCommandExecutor(IOrderRepository orders, IDomainEventPublisher eventPublisher, ITransactionRunner transactions) {
        this.orderRepository = Objects.requireNonNull(orders, "orders must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.transactions = Objects.requireNonNull(transactions, "transactions must not be null");
    }

    /** Loads the order, applies a command to it, and persists the outcome atomically. */
    OrderView apply(UUID orderId, Consumer<Order> command) {
        UUID id = Commands.requireId(orderId, "orderId");
        return transactions.inTransaction(() -> {
            Order order = orderRepository.findById(OrderId.of(id)).orElseThrow(() -> new OrderNotFoundException(id));
            command.accept(order);
            return persist(order);
        });
    }

    /** Persists a brand-new aggregate atomically. */
    OrderView create(Supplier<Order> factory) {
        return transactions.inTransaction(() -> persist(factory.get()));
    }

    private OrderView persist(Order order) {
        Order saved = orderRepository.save(order);
        // Events are drained from the instance that made the decisions: `saved` is rehydrated from
        // the store and so carries the authoritative version, but none of the pending events.
        eventPublisher.publish(order.drainEvents());
        return OrderViews.from(saved);
    }
}
