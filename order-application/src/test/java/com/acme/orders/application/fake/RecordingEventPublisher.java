package com.acme.orders.application.fake;

import com.acme.orders.application.port.out.DomainEventPublisher;
import com.acme.orders.domain.model.shared.DomainEvent;
import java.util.ArrayList;
import java.util.List;

/** Captures published events so tests can assert on what the domain decided, not just on its state. */
public class RecordingEventPublisher implements DomainEventPublisher {

    private final List<DomainEvent> published = new ArrayList<>();

    @Override
    public void publish(List<DomainEvent> events) {
        published.addAll(events);
    }

    public List<DomainEvent> published() {
        return List.copyOf(published);
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> List<T> publishedOfType(Class<T> type) {
        return published.stream().filter(type::isInstance).map(event -> (T) event).toList();
    }

    public void clear() {
        published.clear();
    }
}
