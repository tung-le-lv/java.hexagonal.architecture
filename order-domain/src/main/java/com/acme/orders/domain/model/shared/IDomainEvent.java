package com.acme.orders.domain.model.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * Something that happened in the domain, named in the past tense.
 *
 * <p>Events are created by the aggregate that owns the invariant they witness, and carry the
 * timestamp the caller supplied: the domain never reads the system clock, so behaviour stays
 * deterministic and the core keeps no dependency on ambient infrastructure.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();

    /** Identity of the aggregate instance that emitted the event, as a string. */
    String aggregateId();

    /** Stable logical name used on the wire; overridden when the class name is not the contract. */
    default String eventType() {
        return getClass().getSimpleName();
    }
}
