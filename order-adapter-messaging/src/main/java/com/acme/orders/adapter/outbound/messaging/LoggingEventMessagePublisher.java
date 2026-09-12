package com.acme.orders.adapter.outbound.messaging;

import com.acme.orders.application.port.outbound.EventMessagePublisher;
import com.acme.orders.application.port.outbound.PendingEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Driven adapter that writes events to the log instead of a broker.
 *
 * <p>The point of this class is what it demonstrates rather than what it does: the broker is one
 * replaceable implementation of {@link EventMessagePublisher}, so a Kafka or SQS adapter is a new
 * class in this module and a one-line change in the composition root. Nothing in the domain, the use
 * cases or the outbox moves.
 */
@Component
public class LoggingEventMessagePublisher implements EventMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventMessagePublisher.class);

    @Override
    public void publish(PendingEventMessage message) {
        log.info("publishing event type={} aggregate={}:{} eventId={} occurredAt={} payload={}",
                message.eventType(), message.aggregateType(), message.aggregateId(), message.id(),
                message.occurredAt(), message.payload());
    }
}
