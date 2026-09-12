package com.acme.orders.adapter.outbound.persistence;

import com.acme.orders.adapter.outbound.persistence.entity.OutboxMessageJpaEntity;
import com.acme.orders.adapter.outbound.persistence.repository.IOutboxJpaRepository;
import com.acme.orders.application.port.outbound.IDomainEventPublisher;
import com.acme.orders.domain.event.IDomainEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the event-publishing port by writing to the outbox table.
 *
 * <p>{@link Propagation#MANDATORY} is deliberate: these rows are only safe if they join the caller's
 * transaction, so being called outside one is a programming error that should fail loudly rather than
 * quietly commit events for a write that might still roll back.
 */
@Component
public class OutboxDomainEventPublisher implements IDomainEventPublisher {

    private static final String AGGREGATE_TYPE = "Order";

    private final IOutboxJpaRepository outbox;
    private final ObjectMapper objectMapper;

    public OutboxDomainEventPublisher(IOutboxJpaRepository outbox, ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(List<IDomainEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        outbox.saveAll(events.stream().map(this::toOutboxMessage).toList());
    }

    private OutboxMessageJpaEntity toOutboxMessage(IDomainEvent event) {
        return OutboxMessageJpaEntity.pending(
                event.eventId() == null ? UUID.randomUUID() : event.eventId(),
                AGGREGATE_TYPE,
                event.aggregateId(),
                event.eventType(),
                serialise(event),
                event.occurredAt());
    }

    private String serialise(IDomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException cause) {
            // Jackson 3 throws unchecked, so this catch is a deliberate choice rather than a
            // requirement: an unserialisable event means the write cannot be completed honestly, and
            // naming the event type makes that far easier to diagnose than a bare mapping error.
            throw new IllegalStateException("failed to serialise domain event " + event.eventType(), cause);
        }
    }
}
