package com.acme.orders.bootstrap.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.orders.application.port.inbound.IRelayPendingEventsUseCase;
import com.acme.orders.application.port.outbound.IPendingEventStore;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Drives the whole hexagon through its real driving adapter: HTTP in, database and outbox out.
 *
 * <p>The unit tests prove the rules; this proves the wiring — that the ports are bound to the
 * adapters, the migrations match the mappings, and the transaction boundary actually commits.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IPendingEventStore pendingEvents;

    @Autowired
    private IRelayPendingEventsUseCase relayPendingEvents;

    @Test
    @DisplayName("a customer can assemble, place, pay for and track an order over HTTP")
    void fullPurchaseOverHttp() throws Exception {
        UUID customerId = UUID.randomUUID();

        String orderId = createOrder(customerId);

        // Two units at 120.00 puts the order over the 200.00 tier, so 5% comes off at placement.
        addLine(orderId, "Mechanical keyboard", "120.00", 2);

        mockMvc.perform(post("/api/v1/orders/{id}/placement", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.subtotal").value(240.00))
                .andExpect(jsonPath("$.discount").value(12.00))
                .andExpect(jsonPath("$.total").value(228.00))
                .andExpect(jsonPath("$.placedAt").exists());

        mockMvc.perform(post("/api/v1/orders/{id}/payment", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentReference\":\"stripe-ch-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(post("/api/v1/orders/{id}/shipment", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingNumber\":\"TRACK-99\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.trackingNumber").value("TRACK-99"));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.total").value(228.00))
                .andExpect(jsonPath("$.lines.length()").value(1));

        mockMvc.perform(get("/api/v1/orders").param("customerId", customerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].status").value("SHIPPED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("placing an empty order is a conflict, not a 500")
    void emptyOrderCannotBePlaced() throws Exception {
        String orderId = createOrder(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/orders/{id}/placement", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("order.empty"))
                .andExpect(jsonPath("$.title").value("Order state conflict"));
    }

    @Test
    @DisplayName("editing a placed order is refused with the reason the domain gave")
    void placedOrdersAreFrozen() throws Exception {
        String orderId = createOrder(UUID.randomUUID());
        addLine(orderId, "Trackball", "45.00", 1);
        mockMvc.perform(post("/api/v1/orders/{id}/placement", orderId)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/{id}/lines", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lineJson("Widget", "10.00", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("order.invalid_state"));
    }

    @Test
    @DisplayName("an unknown order is a 404 with a machine-readable code")
    void unknownOrderIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("order.not_found"));
    }

    @Test
    @DisplayName("a malformed request body is rejected at the edge, with the offending fields named")
    void malformedRequestsAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"EU\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.validation_failed"))
                .andExpect(jsonPath("$.errors.customerId").exists())
                .andExpect(jsonPath("$.errors.currency").exists())
                .andExpect(jsonPath("$.errors.shippingAddress").exists());
    }

    @Test
    @DisplayName("a line priced in the wrong currency is a 422, and the order is untouched")
    void currencyMismatchIsUnprocessable() throws Exception {
        String orderId = createOrder(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/orders/{id}/lines", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","productName":"Widget","unitPrice":10.00,"currency":"USD","quantity":1}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("currency.mismatch"));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(jsonPath("$.lines.length()").value(0));
    }

    @Test
    @DisplayName("adding the same product twice merges into one line")
    void repeatedProductsMerge() throws Exception {
        String orderId = createOrder(UUID.randomUUID());
        UUID productId = UUID.randomUUID();

        addLine(orderId, productId, "Widget", "10.00", 2);
        MvcResult result = mockMvc.perform(post("/api/v1/orders/{id}/lines", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lineJson(productId, "Widget", "10.00", 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].quantity").value(5))
                .andExpect(jsonPath("$.total").value(50.00))
                .andReturn();

        assertThat(json(result).at("/totalItemCount").asInt()).isEqualTo(5);
    }

    @Test
    @DisplayName("lines can be changed and removed while the order is a draft")
    void draftLinesCanBeEdited() throws Exception {
        String orderId = createOrder(UUID.randomUUID());
        MvcResult added = mockMvc.perform(post("/api/v1/orders/{id}/lines", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lineJson("Widget", "10.00", 1)))
                .andExpect(status().isOk())
                .andReturn();
        String lineId = json(added).at("/lines/0/id").asText();

        mockMvc.perform(patch("/api/v1/orders/{id}/lines/{lineId}", orderId, lineId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(40.00));

        mockMvc.perform(delete("/api/v1/orders/{id}/lines/{lineId}", orderId, lineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(0));
    }

    @Test
    @DisplayName("placing an order writes its event to the outbox, and the relay drains it")
    void eventsGoThroughTheOutbox() throws Exception {
        long before = pendingEvents.pendingCount();
        String orderId = createOrder(UUID.randomUUID());
        addLine(orderId, "Widget", "10.00", 1);

        mockMvc.perform(post("/api/v1/orders/{id}/placement", orderId)).andExpect(status().isOk());

        assertThat(pendingEvents.pendingCount()).isEqualTo(before + 1);

        // A generous batch: earlier tests in this class share the schema and leave their own events.
        int relayed = relayPendingEvents.relayPendingEvents(100);

        assertThat(relayed).isGreaterThanOrEqualTo(1);
        assertThat(pendingEvents.pendingCount()).isZero();
    }

    // ------------------------------------------------------------------ helpers

    private String createOrder(UUID customerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "currency": "EUR",
                                  "shippingAddress": {
                                    "street": "Kerkstraat 1",
                                    "city": "Amsterdam",
                                    "postalCode": "1012 AB",
                                    "countryCode": "NL"
                                  }
                                }
                                """.formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        return json(result).at("/id").asText();
    }

    private void addLine(String orderId, String name, String price, int quantity) throws Exception {
        addLine(orderId, UUID.randomUUID(), name, price, quantity);
    }

    private void addLine(String orderId, UUID productId, String name, String price, int quantity) throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/lines", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lineJson(productId, name, price, quantity)))
                .andExpect(status().isOk());
    }

    private static String lineJson(String name, String price, int quantity) {
        return lineJson(UUID.randomUUID(), name, price, quantity);
    }

    private static String lineJson(UUID productId, String name, String price, int quantity) {
        return """
                {"productId":"%s","productName":"%s","unitPrice":%s,"currency":"EUR","quantity":%d}
                """.formatted(productId, name, price, quantity);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
