package com.acme.orders.application.usecase;

import com.acme.orders.application.port.inbound.IRelayPendingEventsUseCase;
import com.acme.orders.application.port.outbound.IEventMessagePublisher;
import com.acme.orders.application.port.outbound.PendingEventMessage;
import com.acme.orders.application.port.outbound.IPendingEventStore;
import com.acme.orders.application.port.outbound.ITransactionRunner;
import java.util.List;
import java.util.Objects;

/**
 * Drains the outbox.
 *
 * <p>Each message is settled in its own transaction, so one undeliverable event cannot block the
 * ones behind it, and a crash mid-batch leaves the rest simply still pending. Delivery is therefore
 * at-least-once: a publish that succeeds just before the marking transaction fails will be retried,
 * which is why events carry a stable {@code eventId} for consumers to deduplicate on.
 */
public class RelayPendingEventsService implements IRelayPendingEventsUseCase {

    private final IPendingEventStore pendingEvents;
    private final IEventMessagePublisher publisher;
    private final ITransactionRunner transactions;

    public RelayPendingEventsService(IPendingEventStore pendingEvents, IEventMessagePublisher publisher,
                                     ITransactionRunner transactions) {
        this.pendingEvents = Objects.requireNonNull(pendingEvents, "pendingEvents must not be null");
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
        this.transactions = Objects.requireNonNull(transactions, "transactions must not be null");
    }

    @Override
    public int relayPendingEvents(int batchSize) {
        List<PendingEventMessage> batch = transactions.inTransaction(() -> pendingEvents.nextBatch(batchSize));

        int relayed = 0;
        for (PendingEventMessage message : batch) {
            if (relay(message)) {
                relayed++;
            }
        }
        return relayed;
    }

    private boolean relay(PendingEventMessage message) {
        try {
            publisher.publish(message);
        } catch (RuntimeException failure) {
            transactions.inTransaction(() -> pendingEvents.markFailed(message.id(), describe(failure)));
            return false;
        }
        transactions.inTransaction(() -> pendingEvents.markPublished(message.id()));
        return true;
    }

    private static String describe(RuntimeException failure) {
        return failure.getClass().getSimpleName() + ": " + failure.getMessage();
    }
}
