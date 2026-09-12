package com.acme.orders.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A domain event parked in the database, waiting to be relayed to the broker.
 *
 * <p>The transactional outbox: because this row is written in the same transaction as the order it
 * describes, the two either both commit or both roll back. Publishing straight to a broker instead
 * would leave the familiar dual-write hole — order saved, event lost, or event sent for a
 * transaction that then rolled back.
 */
@Entity
@Table(name = "outbox_messages")
public class OutboxMessageJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    /**
     * The serialised event.
     *
     * <p>A sized VARCHAR rather than a {@code @Lob}: on PostgreSQL a {@code @Lob String} becomes a
     * large-object OID handle instead of readable text, and VARCHAR(n) is valid on every dialect this
     * service is tested against.
     */
    @Column(name = "payload", nullable = false, length = 8192)
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1024)
    private String lastError;

    protected OutboxMessageJpaEntity() {
        // required by JPA
    }

    public static OutboxMessageJpaEntity pending(UUID id, String aggregateType, String aggregateId, String eventType,
                                                 String payload, Instant occurredAt) {
        OutboxMessageJpaEntity entity = new OutboxMessageJpaEntity();
        entity.id = id;
        entity.aggregateType = aggregateType;
        entity.aggregateId = aggregateId;
        entity.eventType = eventType;
        entity.payload = payload;
        entity.occurredAt = occurredAt;
        entity.attempts = 0;
        return entity;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.attempts++;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 1024));
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }
}
