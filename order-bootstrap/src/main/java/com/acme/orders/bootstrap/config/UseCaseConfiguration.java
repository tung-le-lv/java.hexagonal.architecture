package com.acme.orders.bootstrap.config;

import com.acme.orders.application.port.inbound.IAddOrderLineUseCase;
import com.acme.orders.application.port.inbound.ICancelOrderUseCase;
import com.acme.orders.application.port.inbound.IChangeOrderLineQuantityUseCase;
import com.acme.orders.application.port.inbound.ICreateDraftOrderUseCase;
import com.acme.orders.application.port.inbound.IPayOrderUseCase;
import com.acme.orders.application.port.inbound.IPlaceOrderUseCase;
import com.acme.orders.application.port.inbound.IRelayPendingEventsUseCase;
import com.acme.orders.application.port.inbound.IRemoveOrderLineUseCase;
import com.acme.orders.application.port.inbound.IShipOrderUseCase;
import com.acme.orders.application.port.inbound.query.IGetOrderQuery;
import com.acme.orders.application.port.inbound.query.IListCustomerOrdersQuery;
import com.acme.orders.application.port.outbound.IDomainEventPublisher;
import com.acme.orders.application.port.outbound.IEventMessagePublisher;
import com.acme.orders.application.port.outbound.IOrderQueryRepository;
import com.acme.orders.application.port.outbound.IOrderRepository;
import com.acme.orders.application.port.outbound.IPendingEventStore;
import com.acme.orders.application.port.outbound.ITransactionRunner;
import com.acme.orders.application.usecase.AddOrderLineService;
import com.acme.orders.application.usecase.CancelOrderService;
import com.acme.orders.application.usecase.ChangeOrderLineQuantityService;
import com.acme.orders.application.usecase.CreateDraftOrderService;
import com.acme.orders.application.usecase.OrderQueryService;
import com.acme.orders.application.usecase.PayOrderService;
import com.acme.orders.application.usecase.PlaceOrderService;
import com.acme.orders.application.usecase.RelayPendingEventsService;
import com.acme.orders.application.usecase.RemoveOrderLineService;
import com.acme.orders.application.usecase.ShipOrderService;
import com.acme.orders.domain.policy.IDiscountPolicy;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free use cases into the container.
 *
 * <p>Explicit {@code @Bean} methods rather than {@code @Service} on each class: that annotation would
 * put a Spring import inside the application layer, and the whole point of this module is that the
 * dependency on the framework points inward from here and never the other way. The cost is this file;
 * the benefit is a core that compiles and tests without Spring on the classpath at all.
 */
@Configuration(proxyBeanMethods = false)
public class UseCaseConfiguration {

    @Bean
    ICreateDraftOrderUseCase createDraftOrderUseCase(IOrderRepository orders, IDomainEventPublisher events,
                                                    ITransactionRunner transactions, Clock clock) {
        return new CreateDraftOrderService(orders, events, transactions, clock);
    }

    @Bean
    IAddOrderLineUseCase addOrderLineUseCase(IOrderRepository orders, IDomainEventPublisher events,
                                            ITransactionRunner transactions) {
        return new AddOrderLineService(orders, events, transactions);
    }

    @Bean
    IChangeOrderLineQuantityUseCase changeOrderLineQuantityUseCase(IOrderRepository orders, IDomainEventPublisher events,
                                                                  ITransactionRunner transactions) {
        return new ChangeOrderLineQuantityService(orders, events, transactions);
    }

    @Bean
    IRemoveOrderLineUseCase removeOrderLineUseCase(IOrderRepository orders, IDomainEventPublisher events,
                                                  ITransactionRunner transactions) {
        return new RemoveOrderLineService(orders, events, transactions);
    }

    @Bean
    IPlaceOrderUseCase placeOrderUseCase(IOrderRepository orders, IDomainEventPublisher events,
                                        ITransactionRunner transactions, IDiscountPolicy discountPolicy, Clock clock) {
        return new PlaceOrderService(orders, events, transactions, discountPolicy, clock);
    }

    @Bean
    IPayOrderUseCase payOrderUseCase(IOrderRepository orders, IDomainEventPublisher events,
                                    ITransactionRunner transactions, Clock clock) {
        return new PayOrderService(orders, events, transactions, clock);
    }

    @Bean
    IShipOrderUseCase shipOrderUseCase(IOrderRepository orders, IDomainEventPublisher events,
                                      ITransactionRunner transactions, Clock clock) {
        return new ShipOrderService(orders, events, transactions, clock);
    }

    @Bean
    ICancelOrderUseCase cancelOrderUseCase(IOrderRepository orders, IDomainEventPublisher events,
                                          ITransactionRunner transactions, Clock clock) {
        return new CancelOrderService(orders, events, transactions, clock);
    }

    @Bean
    IRelayPendingEventsUseCase relayPendingEventsUseCase(IPendingEventStore pendingEvents,
                                                        IEventMessagePublisher publisher,
                                                        ITransactionRunner transactions) {
        return new RelayPendingEventsService(pendingEvents, publisher, transactions);
    }

    /**
     * One instance satisfies both read ports.
     *
     * <p>Registered once, under its implementation type: callers still inject
     * {@link IGetOrderQuery} or {@link IListCustomerOrdersQuery} and see only the port they need.
     * Registering it again per port would make each port type ambiguous.
     */
    @Bean
    OrderQueryService orderQueryService(IOrderQueryRepository orderQueries) {
        return new OrderQueryService(orderQueries);
    }
}
