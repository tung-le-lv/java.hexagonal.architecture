package com.acme.orders.bootstrap.config;

import com.acme.orders.domain.policy.DiscountPolicy;
import com.acme.orders.domain.policy.TieredVolumeDiscountPolicy;
import com.acme.orders.domain.model.shared.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Currency;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses which domain policies are in force and where time comes from.
 *
 * <p>Configuration, not business logic: the {@code DiscountPolicy} implementations live in the domain,
 * but which one this deployment uses — and with which thresholds — is a deployment decision, so it is
 * made out here. Swapping the policy is a change to this file only; no use case or aggregate moves.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OrderProperties.class)
public class DomainConfiguration {

    /**
     * Injected everywhere a timestamp is needed, so tests can freeze time with
     * {@link Clock#fixed} instead of sleeping.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    DiscountPolicy discountPolicy(OrderProperties properties) {
        Currency currency = Currency.getInstance(properties.discount().currency());
        List<TieredVolumeDiscountPolicy.Tier> tiers = properties.discount().tiers().stream()
                .map(tier -> new TieredVolumeDiscountPolicy.Tier(
                        Money.of(tier.threshold(), currency),
                        BigDecimal.valueOf(tier.percentage())))
                .toList();
        return new TieredVolumeDiscountPolicy(tiers);
    }
}
