package com.acme.orders.application.usecase;

import com.acme.orders.application.port.inbound.AddOrderLineUseCase;
import com.acme.orders.application.port.inbound.command.AddOrderLineCommand;
import com.acme.orders.application.port.outbound.DomainEventPublisher;
import com.acme.orders.application.port.outbound.OrderRepository;
import com.acme.orders.application.port.outbound.TransactionRunner;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.domain.model.order.ProductId;
import com.acme.orders.domain.model.shared.Money;
import com.acme.orders.domain.model.shared.Quantity;
import java.util.Objects;

/** Puts a product on a draft order. Whether that appends a line or merges into one is the aggregate's call. */
public class AddOrderLineService implements AddOrderLineUseCase {

    private final OrderCommandExecutor executor;

    public AddOrderLineService(OrderRepository orders, DomainEventPublisher eventPublisher, TransactionRunner transactions) {
        this.executor = new OrderCommandExecutor(orders, eventPublisher, transactions);
    }

    @Override
    public OrderView addOrderLine(AddOrderLineCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ProductId productId = ProductId.of(Commands.requireId(command.productId(), "productId"));
        Money unitPrice = Money.of(Commands.requireAmount(command.unitPrice(), "unitPrice"),
                Commands.requireCurrency(command.currencyCode()));
        Quantity quantity = Quantity.of(command.quantity());

        String productName = Commands.requireText(command.productName(), "productName");

        return executor.apply(command.orderId(),
                order -> order.addLine(productId, productName, unitPrice, quantity));
    }
}
