package com.acme.orders.application.usecase;

import com.acme.orders.application.port.inbound.IRemoveOrderLineUseCase;
import com.acme.orders.application.port.inbound.command.RemoveOrderLineCommand;
import com.acme.orders.application.port.outbound.IDomainEventPublisher;
import com.acme.orders.application.port.outbound.ITransactionRunner;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.domain.repository.IOrderRepository;
import com.acme.orders.domain.valueobject.OrderLineId;
import java.util.Objects;

/** Takes a line off a draft order. */
public class RemoveOrderLineService implements IRemoveOrderLineUseCase {

    private final OrderCommandExecutor executor;

    public RemoveOrderLineService(IOrderRepository orders, IDomainEventPublisher eventPublisher,
                                  ITransactionRunner transactions) {
        this.executor = new OrderCommandExecutor(orders, eventPublisher, transactions);
    }

    @Override
    public OrderView removeOrderLine(RemoveOrderLineCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        OrderLineId lineId = OrderLineId.of(Commands.requireId(command.orderLineId(), "orderLineId"));

        return executor.apply(command.orderId(), order -> order.removeLine(lineId));
    }
}
