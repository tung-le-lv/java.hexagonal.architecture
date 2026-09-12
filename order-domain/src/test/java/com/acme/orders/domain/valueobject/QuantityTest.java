package com.acme.orders.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.orders.domain.exception.InvalidQuantityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QuantityTest {

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 1001})
    @DisplayName("a quantity outside the allowed range cannot be constructed at all")
    void invalidQuantitiesAreRejected(int value) {
        assertThatThrownBy(() -> Quantity.of(value)).isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    @DisplayName("quantities add up")
    void quantitiesAdd() {
        assertThat(Quantity.of(2).plus(Quantity.of(3))).isEqualTo(Quantity.of(5));
    }

    @Test
    @DisplayName("adding past the maximum is rejected rather than silently capped")
    void addingPastTheMaximumIsRejected() {
        assertThatThrownBy(() -> Quantity.of(1000).plus(Quantity.of(1)))
                .isInstanceOf(InvalidQuantityException.class);
    }
}
