package com.acme.orders.bootstrap.config;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised configuration for the service.
 *
 * <p>Bound here in the bootstrap module rather than anywhere inside the hexagon: Spring Boot's
 * binding annotations are a framework detail, and the core should receive plain values it can
 * validate itself.
 */
@ConfigurationProperties(prefix = "orders")
public record OrderProperties(DiscountProperties discount, OutboxProperties outbox) {

    public OrderProperties {
        discount = discount == null ? DiscountProperties.defaults() : discount;
        outbox = outbox == null ? OutboxProperties.defaults() : outbox;
    }

    public record DiscountProperties(String currency, List<TierProperties> tiers) {

        public DiscountProperties {
            currency = currency == null ? "EUR" : currency;
            tiers = tiers == null ? List.of() : List.copyOf(tiers);
        }

        static DiscountProperties defaults() {
            return new DiscountProperties("EUR", List.of());
        }
    }

    public record TierProperties(BigDecimal threshold, double percentage) {
    }

    public record OutboxProperties(RelayProperties relay) {

        public OutboxProperties {
            relay = relay == null ? new RelayProperties(true, 50, "PT5S") : relay;
        }

        static OutboxProperties defaults() {
            return new OutboxProperties(null);
        }
    }

    public record RelayProperties(boolean enabled, int batchSize, String interval) {
    }
}
