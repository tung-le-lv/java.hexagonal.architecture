package com.acme.orders.application.usecase;

import com.acme.orders.application.port.inbound.PayOrderUseCase;
import com.acme.orders.application.port.inbound.command.PayOrderCommand;
import com.acme.orders.application.port.outbound.DomainEventPublisher;
import com.acme.orders.application.port.outbound.OrderRepository;
import com.acme.orders.application.port.outbound.TransactionRunner;
import com.acme.orders.application.view.OrderView;
import java.time.Clock;
import java.util.Objects;

/** Records confirmed payment against a placed order. */
public class PayOrderService implements PayOrderUseCase {

    private final OrderCommandExecutor executor;
    private final Clock clock;

    public PayOrderService(OrderRepository orders, DomainEventPublisher eventPublisher,
                           TransactionRunner transactions, Clock clock) {
        this.executor = new OrderCommandExecutor(orders, eventPublisher, transactions);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public OrderView payOrder(PayOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String paymentReference = Commands.requireText(command.paymentReference(), "paymentReference");

        return executor.apply(command.orderId(), order -> order.pay(paymentReference, clock.instant()));
    }
}
