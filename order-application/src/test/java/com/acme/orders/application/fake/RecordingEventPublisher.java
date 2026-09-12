package com.acme.orders.application.fake;

import com.acme.orders.application.port.outbound.IDomainEventPublisher;
import com.acme.orders.domain.model.shared.IDomainEvent;
import java.util.ArrayList;
import java.util.List;

/** Captures published events so tests can assert on what the domain decided, not just on its state. */
public class RecordingEventPublisher implements IDomainEventPublisher {

    private final List<IDomainEvent> published = new ArrayList<>();

    @Override
    public void publish(List<IDomainEvent> events) {
        published.addAll(events);
    }

    public List<IDomainEvent> published() {
        return List.copyOf(published);
    }

    @SuppressWarnings("unchecked")
    public <T extends IDomainEvent> List<T> publishedOfType(Class<T> type) {
        return published.stream().filter(type::isInstance).map(event -> (T) event).toList();
    }

    public void clear() {
        published.clear();
    }
}
