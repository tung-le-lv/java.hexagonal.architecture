package com.acme.orders.application.port.outbound;

/**
 * Output port for handing an event to the outside world.
 *
 * <p>Whether that is Kafka, RabbitMQ, SNS or a log line is entirely the adapter's business; the core
 * only requires that a normal return means the event was accepted, and an exception means it was not.
 */
public interface IEventMessagePublisher {

    /**
     * Delivers one event.
     *
     * @throws RuntimeException if delivery failed and should be retried later
     */
    void publish(PendingEventMessage message);
}
