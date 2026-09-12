package com.acme.orders.adapter.out.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal entry point so this adapter's slice tests can boot without the real application.
 *
 * <p>Test-scoped on purpose: the production composition root lives in the bootstrap module, and the
 * adapter must not acquire one of its own just to be testable.
 */
@SpringBootApplication
class PersistenceTestApplication {
}
