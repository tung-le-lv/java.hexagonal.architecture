package com.acme.orders.application.usecase;

import com.acme.orders.application.port.in.ChangeOrderLineQuantityUseCase;
import com.acme.orders.application.port.in.command.ChangeOrderLineQuantityCommand;
import com.acme.orders.application.port.out.DomainEventPublisher;
import com.acme.orders.application.port.out.OrderRepository;
import com.acme.orders.application.port.out.TransactionRunner;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.domain.model.order.OrderLineId;
import com.acme.orders.domain.model.shared.Quantity;
import java.util.Objects;

/** Sets a line's quantity to an exact value. */
public class ChangeOrderLineQuantityService implements ChangeOrderLineQuantityUseCase {

    private final OrderCommandExecutor executor;

    public ChangeOrderLineQuantityService(OrderRepository orders, DomainEventPublisher eventPublisher,
                                          TransactionRunner transactions) {
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
