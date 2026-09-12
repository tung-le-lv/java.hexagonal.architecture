package com.acme.orders.adapter.outbound.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.acme.orders.application.port.inbound.IRelayPendingEventsUseCase;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutboxRelaySchedulerTest {

    @Test
    @DisplayName("each tick asks the use case to drain one batch")
    void delegatesToTheUseCase() {
        AtomicInteger requestedBatchSize = new AtomicInteger();
        IRelayPendingEventsUseCase relay = batchSize -> {
            requestedBatchSize.set(batchSize);
            return batchSize;
        };

        new OutboxRelayScheduler(relay, 25).relay();

        assertThat(requestedBatchSize).hasValue(25);
    }

    @Test
    @DisplayName("a failing run is swallowed, because an escaping exception would kill the schedule")
    void failuresDoNotKillTheSchedule() {
        IRelayPendingEventsUseCase failing = batchSize -> {
            throw new IllegalStateException("broker unavailable");
        };

        OutboxRelayScheduler scheduler = new OutboxRelayScheduler(failing, 10);

        assertThatCode(scheduler::relay).doesNotThrowAnyException();
    }
}
