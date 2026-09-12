package com.acme.orders.application.usecase;

import com.acme.orders.application.port.inbound.CancelOrderUseCase;
import com.acme.orders.application.port.inbound.command.CancelOrderCommand;
import com.acme.orders.application.port.outbound.DomainEventPublisher;
import com.acme.orders.application.port.outbound.OrderRepository;
import com.acme.orders.application.port.outbound.TransactionRunner;
import com.acme.orders.application.view.OrderView;
import java.time.Clock;
import java.util.Objects;

/** Abandons an order. Which statuses still allow that is the aggregate's rule, not this class's. */
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderCommandExecutor executor;
    private final Clock clock;

    public CancelOrderService(OrderRepository orders, DomainEventPublisher eventPublisher,
                              TransactionRunner transactions, Clock clock) {
        this.executor = new OrderCommandExecutor(orders, eventPublisher, transactions);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public OrderView cancelOrder(CancelOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String reason = Commands.requireText(command.reason(), "reason");

        return executor.apply(command.orderId(), order -> order.cancel(reason, clock.instant()));
    }
}
