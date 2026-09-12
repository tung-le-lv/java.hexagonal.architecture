package com.acme.orders.domain.model.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base class for aggregate roots: the only objects an outside caller may hold a reference to,
 * and therefore the only place consistency can be enforced.
 *
 * <p>Events raised while handling a command are buffered here and drained by the application
 * layer after the aggregate has been persisted, which keeps the decision (domain) separate from
 * the delivery (adapter).
 */
public abstract class AggregateRoot<ID> {

    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    public abstract ID id();

    protected final void raise(DomainEvent event) {
        pendingEvents.add(Objects.requireNonNull(event, "event must not be null"));
    }

    /** Events raised so far, without consuming them. */
    public final List<DomainEvent> pendingEvents() {
        return List.copyOf(pendingEvents);
    }

    /** Drains the buffer; called once per transaction by the application layer. */
    public final List<DomainEvent> drainEvents() {
        List<DomainEvent> drained = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return drained;
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !getClass().equals(other.getClass())) {
            return false;
        }
        return Objects.equals(id(), ((AggregateRoot<?>) other).id());
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(id());
    }
}
