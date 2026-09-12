package com.acme.orders.adapter.outbound.messaging;

import com.acme.orders.application.port.inbound.IRelayPendingEventsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Driving adapter: a clock that periodically asks the application to drain the outbox.
 *
 * <p>It holds no logic of its own — the relaying rules live in
 * {@link com.acme.orders.application.usecase.RelayPendingEventsService} — which is what makes the
 * same behaviour reachable from a test or an admin endpoint without a scheduler running.
 */
@Component
@ConditionalOnProperty(name = "orders.outbox.relay.enabled", matchIfMissing = true)
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final IRelayPendingEventsUseCase relayPendingEvents;
    private final int batchSize;

    public OutboxRelayScheduler(IRelayPendingEventsUseCase relayPendingEvents,
                                @Value("${orders.outbox.relay.batch-size:50}") int batchSize) {
        this.relayPendingEvents = relayPendingEvents;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${orders.outbox.relay.interval:PT5S}")
    public void relay() {
        try {
            int relayed = relayPendingEvents.relayPendingEvents(batchSize);
            if (relayed > 0) {
                log.debug("relayed {} outbox message(s)", relayed);
            }
        } catch (RuntimeException failure) {
            // Swallowed on purpose: an exception out of a @Scheduled method kills the schedule, and
            // unrelayed messages stay in the outbox to be retried on the next tick anyway.
            log.warn("outbox relay run failed; will retry on the next tick", failure);
        }
    }
}
