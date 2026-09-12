package com.acme.orders.adapter.outbound.persistence;

import com.acme.orders.adapter.outbound.persistence.entity.OutboxMessageJpaEntity;
import com.acme.orders.adapter.outbound.persistence.repository.IOutboxJpaRepository;
import com.acme.orders.application.port.outbound.PendingEventMessage;
import com.acme.orders.application.port.outbound.IPendingEventStore;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Driven adapter: the outbox table, seen as the store of events awaiting delivery. */
@Component
public class OutboxPendingEventStore implements IPendingEventStore {

    private final IOutboxJpaRepository outbox;
    private final Clock clock;

    public OutboxPendingEventStore(IOutboxJpaRepository outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingEventMessage> nextBatch(int batchSize) {
        return outbox.findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, batchSize)).stream()
                .map(OutboxPendingEventStore::toMessage)
                .toList();
    }

    @Override
    @Transactional
    public void markPublished(UUID messageId) {
        outbox.findById(messageId).ifPresent(message -> message.markPublished(clock.instant()));
    }

    @Override
    @Transactional
    public void markFailed(UUID messageId, String error) {
        outbox.findById(messageId).ifPresent(message -> message.markFailed(error));
    }

    @Override
    @Transactional(readOnly = true)
    public long pendingCount() {
        return outbox.countByPublishedAtIsNull();
    }

    private static PendingEventMessage toMessage(OutboxMessageJpaEntity entity) {
        return new PendingEventMessage(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getOccurredAt());
    }
}
