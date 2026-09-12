package com.acme.orders.application.usecase;

import com.acme.orders.application.port.in.CreateDraftOrderUseCase;
import com.acme.orders.application.port.in.command.CreateDraftOrderCommand;
import com.acme.orders.application.port.out.DomainEventPublisher;
import com.acme.orders.application.port.out.OrderRepository;
import com.acme.orders.application.port.out.TransactionRunner;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.domain.model.order.CustomerId;
import com.acme.orders.domain.model.order.Order;
import com.acme.orders.domain.model.shared.Address;
import java.time.Clock;
import java.util.Currency;
import java.util.Objects;

/**
 * Starts a draft order.
 *
 * <p>Plain constructor injection, no annotations: the composition root in the bootstrap module wires
 * this up, which is what lets the use case be instantiated in a unit test with three hand-written
 * fakes and no container.
 */
public class CreateDraftOrderService implements CreateDraftOrderUseCase {

    private final OrderCommandExecutor executor;
    private final Clock clock;

    public CreateDraftOrderService(OrderRepository orders, DomainEventPublisher eventPublisher,
                                   TransactionRunner transactions, Clock clock) {
        this.executor = new OrderCommandExecutor(orders, eventPublisher, transactions);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public OrderView createDraftOrder(CreateDraftOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        CustomerId customerId = CustomerId.of(Commands.requireId(command.customerId(), "customerId"));
        Currency currency = Commands.requireCurrency(command.currencyCode());
        Address shippingAddress = Address.of(command.street(), command.city(), command.postalCode(), command.countryCode());

        return executor.create(() -> Order.draft(customerId, currency, shippingAddress, clock.instant()));
    }

}
