package com.acme.orders.application.usecase;

import com.acme.orders.application.port.inbound.IPlaceOrderUseCase;
import com.acme.orders.application.port.inbound.command.PlaceOrderCommand;
import com.acme.orders.application.port.outbound.IDomainEventPublisher;
import com.acme.orders.application.port.outbound.IOrderRepository;
import com.acme.orders.application.port.outbound.ITransactionRunner;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.domain.policy.IDiscountPolicy;
import java.time.Clock;
import java.util.Objects;

/**
 * Submits a draft order.
 *
 * <p>The use case supplies the collaborators the decision needs — which discount policy is in force,
 * what time it is — and the aggregate makes the decision. Note what is absent: no pricing arithmetic,
 * no status checks, no "if the order is empty" branch. Those are invariants, so they live in
 * {@link com.acme.orders.domain.model.order.Order#place} where they cannot be bypassed by a second
 * caller that forgets one.
 */
public class PlaceOrderService implements IPlaceOrderUseCase {

    private final OrderCommandExecutor executor;
    private final IDiscountPolicy discountPolicy;
    private final Clock clock;

    public PlaceOrderService(IOrderRepository orders, IDomainEventPublisher eventPublisher,
                             ITransactionRunner transactions, IDiscountPolicy discountPolicy, Clock clock) {
        this.executor = new OrderCommandExecutor(orders, eventPublisher, transactions);
        this.discountPolicy = Objects.requireNonNull(discountPolicy, "discountPolicy must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public OrderView placeOrder(PlaceOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return executor.apply(command.orderId(), order -> order.place(discountPolicy, clock.instant()));
    }
}
