package com.acme.orders.application.usecase;

import com.acme.orders.application.port.inbound.RemoveOrderLineUseCase;
import com.acme.orders.application.port.inbound.command.RemoveOrderLineCommand;
import com.acme.orders.application.port.outbound.DomainEventPublisher;
import com.acme.orders.application.port.outbound.OrderRepository;
import com.acme.orders.application.port.outbound.TransactionRunner;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.domain.model.order.OrderLineId;
import java.util.Objects;

/** Takes a line off a draft order. */
public class RemoveOrderLineService implements RemoveOrderLineUseCase {

    private final OrderCommandExecutor executor;

    public RemoveOrderLineService(OrderRepository orders, DomainEventPublisher eventPublisher,
                                  TransactionRunner transactions) {
        this.executor = new OrderCommandExecutor(orders, eventPublisher, transactions);
    }

    @Override
    public OrderView removeOrderLine(RemoveOrderLineCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        OrderLineId lineId = OrderLineId.of(Commands.requireId(command.orderLineId(), "orderLineId"));

        return executor.apply(command.orderId(), order -> order.removeLine(lineId));
    }
}
