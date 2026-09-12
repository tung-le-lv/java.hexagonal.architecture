package com.acme.orders.application.usecase;

import com.acme.orders.application.port.inbound.ShipOrderUseCase;
import com.acme.orders.application.port.inbound.command.ShipOrderCommand;
import com.acme.orders.application.port.outbound.DomainEventPublisher;
import com.acme.orders.application.port.outbound.OrderRepository;
import com.acme.orders.application.port.outbound.TransactionRunner;
import com.acme.orders.application.view.OrderView;
import java.time.Clock;
import java.util.Objects;

/** Records that a paid order has been handed to the carrier. */
public class ShipOrderService implements ShipOrderUseCase {

    private final OrderCommandExecutor executor;
    private final Clock clock;

    public ShipOrderService(OrderRepository orders, DomainEventPublisher eventPublisher,
                            TransactionRunner transactions, Clock clock) {
        this.executor = new OrderCommandExecutor(orders, eventPublisher, transactions);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public OrderView shipOrder(ShipOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String trackingNumber = Commands.requireText(command.trackingNumber(), "trackingNumber");

        return executor.apply(command.orderId(), order -> order.ship(trackingNumber, clock.instant()));
    }
}
