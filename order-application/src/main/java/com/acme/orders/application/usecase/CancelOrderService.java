package com.acme.orders.application.usecase;

import com.acme.orders.application.port.inbound.ICancelOrderUseCase;
import com.acme.orders.application.port.inbound.command.CancelOrderCommand;
import com.acme.orders.application.port.outbound.IDomainEventPublisher;
import com.acme.orders.application.port.outbound.IOrderRepository;
import com.acme.orders.application.port.outbound.ITransactionRunner;
import com.acme.orders.application.view.OrderView;
import java.time.Clock;
import java.util.Objects;

/** Abandons an order. Which statuses still allow that is the aggregate's rule, not this class's. */
public class CancelOrderService implements ICancelOrderUseCase {

    private final OrderCommandExecutor executor;
    private final Clock clock;

    public CancelOrderService(IOrderRepository orders, IDomainEventPublisher eventPublisher,
                              ITransactionRunner transactions, Clock clock) {
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
