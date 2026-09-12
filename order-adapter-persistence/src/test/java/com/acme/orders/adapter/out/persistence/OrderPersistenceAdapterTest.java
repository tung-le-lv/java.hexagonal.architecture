package com.acme.orders.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.orders.application.exception.ConcurrentModificationException;
import com.acme.orders.application.view.OrderSummaryView;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.application.view.Page;
import com.acme.orders.domain.model.order.CustomerId;
import com.acme.orders.domain.model.order.Order;
import com.acme.orders.domain.model.order.OrderLineId;
import com.acme.orders.domain.model.order.OrderStatus;
import com.acme.orders.domain.model.order.ProductId;
import com.acme.orders.domain.model.shared.Address;
import com.acme.orders.domain.model.shared.Money;
import com.acme.orders.domain.model.shared.Quantity;
import com.acme.orders.domain.policy.NoDiscountPolicy;
import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies that the aggregate survives a round trip through the database unchanged.
 *
 * <p>The mapping is the riskiest part of the hexagon's outbound edge — it is hand-written precisely so
 * the model stays clean — so it is tested against a real database rather than asserted by inspection.
 */
@DataJpaTest
@Import({OrderPersistenceAdapter.class})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class OrderPersistenceAdapterTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Instant CREATED_AT = Instant.parse("2026-03-01T12:00:00Z");

    @Autowired
    private OrderPersistenceAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("an order round-trips through the database with its state intact")
    void roundTripsFaithfully() {
        Order order = draftWithTwoLines();
        order.place(new NoDiscountPolicy(), CREATED_AT.plusSeconds(60));

        adapter.save(order);
        flushAndClear();

        Order loaded = adapter.findById(order.id()).orElseThrow();

        assertThat(loaded.id()).isEqualTo(order.id());
        assertThat(loaded.customerId()).isEqualTo(order.customerId());
        assertThat(loaded.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(loaded.currency()).isEqualTo(EUR);
        assertThat(loaded.shippingAddress()).isEqualTo(order.shippingAddress());
        assertThat(loaded.subtotal()).isEqualTo(order.subtotal());
        assertThat(loaded.total()).isEqualTo(order.total());
        assertThat(loaded.createdAt()).isEqualTo(CREATED_AT);
        assertThat(loaded.placedAt()).contains(CREATED_AT.plusSeconds(60));
        assertThat(loaded.lines()).hasSize(2);
        assertThat(loaded.lines())
                .extracting(line -> line.unitPrice().amount().toPlainString())
                .containsExactlyInAnyOrder("79.99", "45.50");
    }

    @Test
    @DisplayName("an insert is version 0, and each subsequent write advances it")
    void versionAdvancesOnEveryWrite() {
        Order order = draftWithTwoLines();

        Order inserted = adapter.save(order);
        assertThat(inserted.version()).isZero();

        flushAndClear();
        Order reloaded = adapter.findById(order.id()).orElseThrow();
        reloaded.changeLineQuantity(reloaded.lines().get(0).id(), Quantity.of(5));

        assertThat(adapter.save(reloaded).version()).isEqualTo(1L);
    }

    @Test
    @DisplayName("removing a line deletes its row rather than orphaning it")
    void removedLinesAreDeleted() {
        Order order = draftWithTwoLines();
        Order saved = adapter.save(order);
        flushAndClear();
        OrderLineId removed = saved.lines().get(0).id();

        saved.removeLine(removed);
        adapter.save(saved);
        flushAndClear();

        Order reloaded = adapter.findById(order.id()).orElseThrow();
        assertThat(reloaded.lines()).hasSize(1);
        assertThat(reloaded.lines()).extracting(line -> line.id()).doesNotContain(removed);
    }

    @Test
    @DisplayName("a quantity change updates the existing line instead of replacing it")
    void quantityChangesUpdateInPlace() {
        Order order = draftWithTwoLines();
        Order saved = adapter.save(order);
        flushAndClear();
        OrderLineId lineId = saved.lines().get(0).id();

        saved.changeLineQuantity(lineId, Quantity.of(7));
        adapter.save(saved);
        flushAndClear();

        Order reloaded = adapter.findById(order.id()).orElseThrow();
        assertThat(reloaded.lines()).hasSize(2);
        assertThat(reloaded.lines().stream().filter(line -> line.id().equals(lineId)).findFirst().orElseThrow()
                .quantity()).isEqualTo(Quantity.of(7));
    }

    @Test
    @DisplayName("saving a stale aggregate is refused instead of overwriting the newer state")
    void staleWritesAreRefused() {
        Order order = draftWithTwoLines();
        Order firstLoad = adapter.save(order);
        flushAndClear();
        // A second caller reads the same order, changes it, and commits first.
        Order secondLoad = adapter.findById(order.id()).orElseThrow();
        secondLoad.changeLineQuantity(secondLoad.lines().get(0).id(), Quantity.of(4));
        adapter.save(secondLoad);
        flushAndClear();

        firstLoad.changeLineQuantity(firstLoad.lines().get(0).id(), Quantity.of(9));

        assertThatThrownBy(() -> adapter.save(firstLoad))
                .isInstanceOf(ConcurrentModificationException.class)
                .hasMessageContaining("reload it and retry");
    }

    @Test
    @DisplayName("the read side projects straight from rows, deriving the same totals")
    void readSideProjection() {
        Order order = draftWithTwoLines();
        order.place(new NoDiscountPolicy(), CREATED_AT.plusSeconds(60));
        adapter.save(order);
        flushAndClear();

        Optional<OrderView> view = adapter.findById(order.id().value());

        assertThat(view).isPresent();
        assertThat(view.get().status()).isEqualTo("PLACED");
        assertThat(view.get().currency()).isEqualTo("EUR");
        assertThat(view.get().lines()).hasSize(2);
        assertThat(view.get().subtotal()).isEqualByComparingTo(order.subtotal().amount());
        assertThat(view.get().total()).isEqualByComparingTo(order.total().amount());
        assertThat(view.get().totalItemCount()).isEqualTo(order.totalItemCount());
    }

    @Test
    @DisplayName("a customer's orders are paged, newest first")
    void listsACustomersOrdersNewestFirst() {
        CustomerId customerId = CustomerId.of(UUID.randomUUID());
        Order older = draftFor(customerId, CREATED_AT);
        Order newer = draftFor(customerId, CREATED_AT.plusSeconds(3600));
        adapter.save(older);
        adapter.save(newer);
        flushAndClear();

        Page<OrderSummaryView> page = adapter.findByCustomerId(customerId.value(), 0, 10);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(OrderSummaryView::orderId)
                .containsExactly(newer.id().value(), older.id().value());
        assertThat(page.hasNext()).isFalse();
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("paging reports the next page when there is one")
    void pagingReportsMorePages() {
        CustomerId customerId = CustomerId.of(UUID.randomUUID());
        for (int i = 0; i < 3; i++) {
            adapter.save(draftFor(customerId, CREATED_AT.plusSeconds(i)));
        }
        flushAndClear();

        Page<OrderSummaryView> firstPage = adapter.findByCustomerId(customerId.value(), 0, 2);

        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();
    }

    @Test
    @DisplayName("an order that was never saved is simply absent")
    void missingOrdersAreEmpty() {
        assertThat(adapter.findById(com.acme.orders.domain.model.order.OrderId.newId())).isEmpty();
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }

    /**
     * Pushes pending writes to the database and empties the persistence context, so the reload that
     * follows genuinely comes from SQL rather than from Hibernate's first-level cache — without this
     * the round-trip assertions would pass even if the mapping were broken.
     */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private static Order draftWithTwoLines() {
        Order order = draftFor(CustomerId.of(UUID.randomUUID()), CREATED_AT);
        order.addLine(ProductId.of(UUID.randomUUID()), "Mechanical keyboard", Money.of("79.99", "EUR"), Quantity.of(2));
        order.addLine(ProductId.of(UUID.randomUUID()), "Trackball", Money.of("45.50", "EUR"), Quantity.of(1));
        return order;
    }

    private static Order draftFor(CustomerId customerId, Instant createdAt) {
        return Order.draft(customerId, EUR, Address.of("Kerkstraat 1", "Amsterdam", "1012 AB", "NL"), createdAt);
    }
}
