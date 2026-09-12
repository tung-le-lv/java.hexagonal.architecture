package com.acme.orders.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.orders.domain.exception.CurrencyMismatchException;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    @DisplayName("amounts are normalised to the currency's minor unit, so equality is reliable")
    void amountsAreNormalised() {
        assertThat(Money.of("10.5", "EUR")).isEqualTo(Money.of("10.50", "EUR"));
        assertThat(Money.of(new BigDecimal("10"), EUR).amount()).isEqualByComparingTo("10.00");
        assertThat(Money.of("10.005", "EUR")).isEqualTo(Money.of("10.01", "EUR"));
    }

    @Test
    @DisplayName("currencies with no minor unit keep no decimals")
    void zeroDecimalCurrencies() {
        assertThat(Money.of("1000.4", "JPY").amount()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("arithmetic stays within one currency")
    void arithmetic() {
        assertThat(Money.of("10.00", "EUR").add(Money.of("2.50", "EUR"))).isEqualTo(Money.of("12.50", "EUR"));
        assertThat(Money.of("10.00", "EUR").subtract(Money.of("2.50", "EUR"))).isEqualTo(Money.of("7.50", "EUR"));
        assertThat(Money.of("10.00", "EUR").multiply(3)).isEqualTo(Money.of("30.00", "EUR"));
        assertThat(Money.of("200.00", "EUR").percentage(new BigDecimal("5"))).isEqualTo(Money.of("10.00", "EUR"));
    }

    @Test
    @DisplayName("mixing currencies is an error rather than a meaningless number")
    void mixingCurrenciesIsRejected() {
        Money euros = Money.of("10.00", "EUR");
        Money dollars = Money.of("10.00", "USD");

        assertThatThrownBy(() -> euros.add(dollars)).isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> euros.subtract(dollars)).isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> euros.compareTo(dollars)).isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    @DisplayName("comparisons read the way the business states the rule")
    void comparisons() {
        assertThat(Money.of("10.00", "EUR").isGreaterThan(Money.of("9.99", "EUR"))).isTrue();
        assertThat(Money.of("10.00", "EUR").isLessThan(Money.of("10.01", "EUR"))).isTrue();
        assertThat(Money.zero(EUR).isZero()).isTrue();
        assertThat(Money.of("-0.01", "EUR").isNegative()).isTrue();
    }
}
