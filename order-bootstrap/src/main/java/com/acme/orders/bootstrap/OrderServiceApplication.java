package com.acme.orders.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point and composition root.
 *
 * <p>This is the only class that knows the whole hexagon exists. Component scanning is pointed
 * explicitly at the adapter packages rather than at {@code com.acme.orders}, so the pure modules stay
 * outside the container's reach and an accidental {@code @Component} in the domain would have no
 * effect — the architecture is enforced by the wiring, not by convention.
 */
@SpringBootApplication(scanBasePackages = {
        "com.acme.orders.bootstrap",
        "com.acme.orders.adapter.inbound.rest",
        "com.acme.orders.adapter.outbound.persistence",
        "com.acme.orders.adapter.outbound.messaging"
})
@EnableJpaRepositories(basePackages = "com.acme.orders.adapter.outbound.persistence.repository")
@EntityScan(basePackages = "com.acme.orders.adapter.outbound.persistence.entity")
@EnableScheduling
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
