package com.acme.orders.adapter.outbound.persistence;

import com.acme.orders.application.port.outbound.TransactionRunner;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Implements the application's transaction boundary with Spring's {@link TransactionTemplate}.
 *
 * <p>This is the adapter that makes {@code @Transactional} on the use cases unnecessary: the core
 * says "this work is one transaction" through the port, and only this class knows what that means
 * technically. Swapping to a different transaction manager is a change to this file alone.
 */
@Component
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionRunner(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }
}
