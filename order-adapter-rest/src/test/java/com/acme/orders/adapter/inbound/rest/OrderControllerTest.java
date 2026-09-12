package com.acme.orders.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.orders.application.exception.ConcurrentModificationException;
import com.acme.orders.application.exception.OrderNotFoundException;
import com.acme.orders.application.port.inbound.IAddOrderLineUseCase;
import com.acme.orders.application.port.inbound.ICancelOrderUseCase;
import com.acme.orders.application.port.inbound.IChangeOrderLineQuantityUseCase;
import com.acme.orders.application.port.inbound.ICreateDraftOrderUseCase;
import com.acme.orders.application.port.inbound.IPayOrderUseCase;
import com.acme.orders.application.port.inbound.IPlaceOrderUseCase;
import com.acme.orders.application.port.inbound.IRemoveOrderLineUseCase;
import com.acme.orders.application.port.inbound.IShipOrderUseCase;
import com.acme.orders.application.port.inbound.command.CreateDraftOrderCommand;
import com.acme.orders.application.port.inbound.command.PlaceOrderCommand;
import com.acme.orders.application.port.inbound.query.IGetOrderQuery;
import com.acme.orders.application.port.inbound.query.IListCustomerOrdersQuery;
import com.acme.orders.application.view.OrderSummaryView;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.application.view.Page;
import com.acme.orders.domain.exception.EmptyOrderException;
import com.acme.orders.domain.exception.InvalidOrderStateException;
import com.acme.orders.domain.valueobject.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests the inbound adapter on its own: request binding, validation, mapping, and the translation of
 * core failures into status codes.
 *
 * <p>The use cases are replaced with hand-written stubs, which is possible only because the controller
 * depends on ports rather than on implementations. Nothing here needs a database.
 */
@WebMvcTest(controllers = OrderController.class)
@Import(OrderControllerTest.StubbedUseCases.class)
class OrderControllerTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StubbedUseCases.Stub stub;

    @BeforeEach
    void resetStub() {
        stub.reset();
    }

    @Test
    @DisplayName("a created order answers 201 with a Location header")
    void createReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "currency": "EUR",
                                  "shippingAddress": {
                                    "street": "Kerkstraat 1", "city": "Amsterdam",
                                    "postalCode": "1012 AB", "countryCode": "NL"
                                  }
                                }
                                """.formatted(CUSTOMER_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/orders/" + ORDER_ID))
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("the request is translated into a command without the transport leaking through")
    void requestBecomesACommand() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "currency": "eur",
                                  "shippingAddress": {
                                    "street": "Kerkstraat 1", "city": "Amsterdam",
                                    "postalCode": "1012 AB", "countryCode": "NL"
                                  }
                                }
                                """.formatted(CUSTOMER_ID)))
                .andExpect(status().isCreated());

        CreateDraftOrderCommand command = stub.lastCreateCommand.get();
        assertThat(command).isNotNull();
        assertThat(command.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(command.currencyCode()).isEqualTo("eur");
        assertThat(command.city()).isEqualTo("Amsterdam");
    }

    @Test
    @DisplayName("an invalid body is rejected with the offending fields, without reaching a use case")
    void validationFailsFast() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"EURO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.validation_failed"))
                .andExpect(jsonPath("$.errors.customerId").exists())
                .andExpect(jsonPath("$.errors.currency").exists());

        assertThat(stub.lastCreateCommand.get()).isNull();
    }

    @Test
    @DisplayName("a non-positive quantity is rejected at the edge")
    void quantityIsValidatedAtTheEdge() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/lines", ORDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","productName":"Widget","unitPrice":10.00,"currency":"EUR","quantity":0}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quantity").exists());
    }

    @Test
    @DisplayName("a malformed UUID in the path is a 400, not a 500")
    void malformedPathVariableIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.malformed_parameter"));
    }

    @Test
    @DisplayName("a missing required query parameter is a 400")
    void missingQueryParameterIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.missing_parameter"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("customerId")));
    }

    @Test
    @DisplayName("an unparseable body is a 400, and says nothing about the parser")
    void unreadableBodyIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.malformed_body"));
    }

    @Test
    @DisplayName("OrderNotFound becomes 404 with its code preserved")
    void notFoundBecomes404() throws Exception {
        stub.getOrderBehaviour = id -> {
            throw new OrderNotFoundException(id);
        };

        mockMvc.perform(get("/api/v1/orders/{id}", ORDER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("order.not_found"))
                .andExpect(jsonPath("$.detail").value("order " + ORDER_ID + " was not found"));
    }

    @Test
    @DisplayName("an illegal transition becomes 409, because the request conflicts with current state")
    void invalidStateBecomes409() throws Exception {
        stub.placeBehaviour = id -> {
            throw new InvalidOrderStateException("place", OrderStatus.PAID, OrderStatus.SHIPPED);
        };

        mockMvc.perform(post("/api/v1/orders/{id}/placement", ORDER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("order.invalid_state"));
    }

    @Test
    @DisplayName("an empty order becomes 409")
    void emptyOrderBecomes409() throws Exception {
        stub.placeBehaviour = id -> {
            throw new EmptyOrderException(id.toString());
        };

        mockMvc.perform(post("/api/v1/orders/{id}/placement", ORDER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("order.empty"));
    }

    @Test
    @DisplayName("a concurrent modification becomes 409 and tells the client to retry")
    void concurrentModificationBecomes409() throws Exception {
        stub.placeBehaviour = id -> {
            throw new ConcurrentModificationException(id, null);
        };

        mockMvc.perform(post("/api/v1/orders/{id}/placement", ORDER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("order.concurrent_modification"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("retry")));
    }

    @Test
    @DisplayName("an unexpected failure becomes 500 without leaking its message")
    void unexpectedFailureIsOpaque() throws Exception {
        stub.placeBehaviour = id -> {
            throw new IllegalStateException("connection string: user=root password=hunter2");
        };

        mockMvc.perform(post("/api/v1/orders/{id}/placement", ORDER_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("internal_error"))
                .andExpect(jsonPath("$.detail").value("the request could not be completed"));
    }

    @Test
    @DisplayName("the order list is returned with paging metadata")
    void listIncludesPagingMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/orders").param("customerId", CUSTOMER_ID.toString())
                        .param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    /**
     * Stub implementations of every input port the controller drives. Deliberately hand-written rather
     * than mocked: the ports are small enough that a stub is clearer than a mocking DSL, and it keeps
     * the test honest about what the controller is allowed to call.
     */
    @TestConfiguration
    static class StubbedUseCases {

        static class Stub {

            final AtomicReference<CreateDraftOrderCommand> lastCreateCommand = new AtomicReference<>();
            final AtomicReference<PlaceOrderCommand> lastPlaceCommand = new AtomicReference<>();

            Function<UUID, OrderView> getOrderBehaviour = id -> view(OrderStatus.DRAFT);
            Function<UUID, OrderView> placeBehaviour = id -> view(OrderStatus.PLACED);

            void reset() {
                lastCreateCommand.set(null);
                lastPlaceCommand.set(null);
                getOrderBehaviour = id -> view(OrderStatus.DRAFT);
                placeBehaviour = id -> view(OrderStatus.PLACED);
            }
        }

        @Bean
        Stub stub() {
            return new Stub();
        }

        @Bean
        ICreateDraftOrderUseCase createDraftOrderUseCase(Stub stub) {
            return command -> {
                stub.lastCreateCommand.set(command);
                return view(OrderStatus.DRAFT);
            };
        }

        @Bean
        IAddOrderLineUseCase addOrderLineUseCase() {
            return command -> view(OrderStatus.DRAFT);
        }

        @Bean
        IChangeOrderLineQuantityUseCase changeOrderLineQuantityUseCase() {
            return command -> view(OrderStatus.DRAFT);
        }

        @Bean
        IRemoveOrderLineUseCase removeOrderLineUseCase() {
            return command -> view(OrderStatus.DRAFT);
        }

        @Bean
        IPlaceOrderUseCase placeOrderUseCase(Stub stub) {
            return command -> {
                stub.lastPlaceCommand.set(command);
                return stub.placeBehaviour.apply(command.orderId());
            };
        }

        @Bean
        IPayOrderUseCase payOrderUseCase() {
            return command -> view(OrderStatus.PAID);
        }

        @Bean
        IShipOrderUseCase shipOrderUseCase() {
            return command -> view(OrderStatus.SHIPPED);
        }

        @Bean
        ICancelOrderUseCase cancelOrderUseCase() {
            return command -> view(OrderStatus.CANCELLED);
        }

        @Bean
        IGetOrderQuery getOrderQuery(Stub stub) {
            return orderId -> stub.getOrderBehaviour.apply(orderId);
        }

        @Bean
        IListCustomerOrdersQuery listCustomerOrdersQuery() {
            return (customerId, page, size) -> Page.of(List.of(new OrderSummaryView(
                    ORDER_ID, customerId, "PLACED", "EUR", new BigDecimal("100.00"), 1,
                    Instant.parse("2026-03-01T12:00:00Z"), null)), page, size, 3);
        }

        static OrderView view(OrderStatus status) {
            return new OrderView(
                    ORDER_ID, CUSTOMER_ID, status.name(), "EUR",
                    new OrderView.AddressView("Kerkstraat 1", "Amsterdam", "1012 AB", "NL"),
                    List.of(new OrderView.OrderLineView(UUID.randomUUID(), UUID.randomUUID(), "Widget", 1,
                            new BigDecimal("100.00"), new BigDecimal("100.00"))),
                    new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"), 1,
                    Instant.parse("2026-03-01T12:00:00Z"), null, null, null, null, null, null, null, 1L);
        }
    }
}
