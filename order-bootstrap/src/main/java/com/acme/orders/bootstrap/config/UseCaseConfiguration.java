package com.acme.orders.bootstrap.config;

import com.acme.orders.application.port.inbound.AddOrderLineUseCase;
import com.acme.orders.application.port.inbound.CancelOrderUseCase;
import com.acme.orders.application.port.inbound.ChangeOrderLineQuantityUseCase;
import com.acme.orders.application.port.inbound.CreateDraftOrderUseCase;
import com.acme.orders.application.port.inbound.PayOrderUseCase;
import com.acme.orders.application.port.inbound.PlaceOrderUseCase;
import com.acme.orders.application.port.inbound.RelayPendingEventsUseCase;
import com.acme.orders.application.port.inbound.RemoveOrderLineUseCase;
import com.acme.orders.application.port.inbound.ShipOrderUseCase;
import com.acme.orders.application.port.inbound.query.GetOrderQuery;
import com.acme.orders.application.port.inbound.query.ListCustomerOrdersQuery;
import com.acme.orders.application.port.outbound.DomainEventPublisher;
import com.acme.orders.application.port.outbound.EventMessagePublisher;
import com.acme.orders.application.port.outbound.OrderQueryRepository;
import com.acme.orders.application.port.outbound.OrderRepository;
import com.acme.orders.application.port.outbound.PendingEventStore;
import com.acme.orders.application.port.outbound.TransactionRunner;
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
import com.acme.orders.domain.policy.DiscountPolicy;
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
    CreateDraftOrderUseCase createDraftOrderUseCase(OrderRepository orders, DomainEventPublisher events,
                                                    TransactionRunner transactions, Clock clock) {
        return new CreateDraftOrderService(orders, events, transactions, clock);
    }

    @Bean
    AddOrderLineUseCase addOrderLineUseCase(OrderRepository orders, DomainEventPublisher events,
                                            TransactionRunner transactions) {
        return new AddOrderLineService(orders, events, transactions);
    }

    @Bean
    ChangeOrderLineQuantityUseCase changeOrderLineQuantityUseCase(OrderRepository orders, DomainEventPublisher events,
                                                                  TransactionRunner transactions) {
        return new ChangeOrderLineQuantityService(orders, events, transactions);
    }

    @Bean
    RemoveOrderLineUseCase removeOrderLineUseCase(OrderRepository orders, DomainEventPublisher events,
                                                  TransactionRunner transactions) {
        return new RemoveOrderLineService(orders, events, transactions);
    }

    @Bean
    PlaceOrderUseCase placeOrderUseCase(OrderRepository orders, DomainEventPublisher events,
                                        TransactionRunner transactions, DiscountPolicy discountPolicy, Clock clock) {
        return new PlaceOrderService(orders, events, transactions, discountPolicy, clock);
    }

    @Bean
    PayOrderUseCase payOrderUseCase(OrderRepository orders, DomainEventPublisher events,
                                    TransactionRunner transactions, Clock clock) {
        return new PayOrderService(orders, events, transactions, clock);
    }

    @Bean
    ShipOrderUseCase shipOrderUseCase(OrderRepository orders, DomainEventPublisher events,
                                      TransactionRunner transactions, Clock clock) {
        return new ShipOrderService(orders, events, transactions, clock);
    }

    @Bean
    CancelOrderUseCase cancelOrderUseCase(OrderRepository orders, DomainEventPublisher events,
                                          TransactionRunner transactions, Clock clock) {
        return new CancelOrderService(orders, events, transactions, clock);
    }

    @Bean
    RelayPendingEventsUseCase relayPendingEventsUseCase(PendingEventStore pendingEvents,
                                                        EventMessagePublisher publisher,
                                                        TransactionRunner transactions) {
        return new RelayPendingEventsService(pendingEvents, publisher, transactions);
    }

    /**
     * One instance satisfies both read ports.
     *
     * <p>Registered once, under its implementation type: callers still inject
     * {@link GetOrderQuery} or {@link ListCustomerOrdersQuery} and see only the port they need.
     * Registering it again per port would make each port type ambiguous.
     */
    @Bean
    OrderQueryService orderQueryService(OrderQueryRepository orderQueries) {
        return new OrderQueryService(orderQueries);
    }
}
