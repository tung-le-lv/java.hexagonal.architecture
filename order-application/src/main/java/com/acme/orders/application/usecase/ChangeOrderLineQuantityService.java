package com.acme.orders.application.usecase;

import com.acme.orders.application.port.inbound.IChangeOrderLineQuantityUseCase;
import com.acme.orders.application.port.inbound.command.ChangeOrderLineQuantityCommand;
import com.acme.orders.application.port.outbound.IDomainEventPublisher;
import com.acme.orders.application.port.outbound.IOrderRepository;
import com.acme.orders.application.port.outbound.ITransactionRunner;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.domain.model.order.OrderLineId;
import com.acme.orders.domain.model.shared.Quantity;
import java.util.Objects;

/** Sets a line's quantity to an exact value. */
public class ChangeOrderLineQuantityService implements IChangeOrderLineQuantityUseCase {

    private final OrderCommandExecutor executor;

    public ChangeOrderLineQuantityService(IOrderRepository orders, IDomainEventPublisher eventPublisher,
                                          ITransactionRunner transactions) {
        this.executor = new OrderCommandExecutor(orders, eventPublisher, transactions);
    }

    @Override
    public OrderView changeOrderLineQuantity(ChangeOrderLineQuantityCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        OrderLineId lineId = OrderLineId.of(Commands.requireId(command.orderLineId(), "orderLineId"));
        Quantity quantity = Quantity.of(command.quantity());

        return executor.apply(command.orderId(), order -> order.changeLineQuantity(lineId, quantity));
    }
}
