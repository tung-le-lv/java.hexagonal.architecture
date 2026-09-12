package com.acme.orders.application.usecase;

import com.acme.orders.application.port.inbound.IPayOrderUseCase;
import com.acme.orders.application.port.inbound.command.PayOrderCommand;
import com.acme.orders.application.port.outbound.IDomainEventPublisher;
import com.acme.orders.application.port.outbound.IOrderRepository;
import com.acme.orders.application.port.outbound.ITransactionRunner;
import com.acme.orders.application.view.OrderView;
import java.time.Clock;
import java.util.Objects;

/** Records confirmed payment against a placed order. */
public class PayOrderService implements IPayOrderUseCase {

    private final OrderCommandExecutor executor;
    private final Clock clock;

    public PayOrderService(IOrderRepository orders, IDomainEventPublisher eventPublisher,
                           ITransactionRunner transactions, Clock clock) {
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
