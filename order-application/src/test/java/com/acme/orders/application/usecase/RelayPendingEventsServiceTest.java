package com.acme.orders.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.orders.application.fake.DirectTransactionRunner;
import com.acme.orders.application.port.outbound.IEventMessagePublisher;
import com.acme.orders.application.port.outbound.PendingEventMessage;
import com.acme.orders.application.port.outbound.IPendingEventStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RelayPendingEventsServiceTest {

    private final FakePendingEventStore store = new FakePendingEventStore();
    private final FakePublisher publisher = new FakePublisher();
    private final RelayPendingEventsService relay =
            new RelayPendingEventsService(store, publisher, new DirectTransactionRunner());

    @Test
    @DisplayName("pending events are published and marked, oldest first")
    void relaysPendingEvents() {
        store.add("OrderPlaced");
        store.add("OrderPaid");

        int relayed = relay.relayPendingEvents(10);

        assertThat(relayed).isEqualTo(2);
        assertThat(publisher.sent).extracting(PendingEventMessage::eventType)
                .containsExactly("OrderPlaced", "OrderPaid");
        assertThat(store.published).hasSize(2);
        assertThat(store.failures).isEmpty();
    }

    @Test
    @DisplayName("one undeliverable event is recorded as failed and does not block the rest")
    void oneFailureDoesNotBlockTheBatch() {
        UUID poison = store.add("OrderPlaced");
        store.add("OrderPaid");
        publisher.failOn(poison);

        int relayed = relay.relayPendingEvents(10);

        assertThat(relayed).isEqualTo(1);
        assertThat(store.failures).containsOnlyKeys(poison);
        assertThat(store.failures.get(poison)).contains("broker unavailable");
        assertThat(store.published).doesNotContain(poison);
    }

    @Test
    @DisplayName("a failed event stays pending, so delivery is retried rather than lost")
    void failedEventsRemainPending() {
        UUID poison = store.add("OrderPlaced");
        publisher.failOn(poison);

        relay.relayPendingEvents(10);

        assertThat(store.nextBatch(10)).extracting(PendingEventMessage::id).contains(poison);
    }

    @Test
    @DisplayName("an empty outbox is a no-op")
    void emptyOutboxIsANoOp() {
        assertThat(relay.relayPendingEvents(10)).isZero();
        assertThat(publisher.sent).isEmpty();
    }

    private static final class FakePendingEventStore implements IPendingEventStore {

        private final Map<UUID, PendingEventMessage> pending = new LinkedHashMap<>();
        private final List<UUID> published = new ArrayList<>();
        private final Map<UUID, String> failures = new LinkedHashMap<>();
        private Instant nextOccurredAt = Instant.parse("2026-03-01T12:00:00Z");

        UUID add(String eventType) {
            UUID id = UUID.randomUUID();
            pending.put(id, new PendingEventMessage(id, "Order", UUID.randomUUID().toString(), eventType,
                    "{}", nextOccurredAt));
            nextOccurredAt = nextOccurredAt.plusSeconds(1);
            return id;
        }

        @Override
        public List<PendingEventMessage> nextBatch(int batchSize) {
            return pending.values().stream().limit(batchSize).toList();
        }

        @Override
        public void markPublished(UUID messageId) {
            published.add(messageId);
            pending.remove(messageId);
        }

        @Override
        public void markFailed(UUID messageId, String error) {
            failures.put(messageId, error);
        }

        @Override
        public long pendingCount() {
            return pending.size();
        }
    }

    private static final class FakePublisher implements IEventMessagePublisher {

        private final List<PendingEventMessage> sent = new ArrayList<>();
        private UUID failingId;

        void failOn(UUID messageId) {
            this.failingId = messageId;
        }

        @Override
        public void publish(PendingEventMessage message) {
            if (message.id().equals(failingId)) {
                throw new IllegalStateException("broker unavailable");
            }
            sent.add(message);
        }
    }
}
