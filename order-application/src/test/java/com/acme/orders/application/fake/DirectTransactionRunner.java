package com.acme.orders.application.fake;

import com.acme.orders.application.port.out.TransactionRunner;
import java.util.function.Supplier;

/**
 * Runs the work immediately, with no transaction.
 *
 * <p>Adequate for use case tests precisely because the port hides the mechanism: the use case under
 * test cannot tell the difference, which is the property that makes it testable without a database.
 */
public class DirectTransactionRunner implements TransactionRunner {

    private int invocations;

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        invocations++;
        return work.get();
    }

    /** How many transactions the use case opened — each command should need exactly one. */
    public int invocations() {
        return invocations;
    }
}
