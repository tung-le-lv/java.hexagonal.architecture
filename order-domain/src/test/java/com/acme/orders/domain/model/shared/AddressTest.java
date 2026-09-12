package com.acme.orders.domain.model.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.orders.domain.exception.InvalidAddressException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AddressTest {

    @Test
    @DisplayName("an address is normalised on construction")
    void normalisesInput() {
        Address address = Address.of("  Kerkstraat 1 ", " Amsterdam", "1012 AB", "nl");

        assertThat(address.street()).isEqualTo("Kerkstraat 1");
        assertThat(address.city()).isEqualTo("Amsterdam");
        assertThat(address.countryCode()).isEqualTo("NL");
    }

    @Test
    @DisplayName("an incomplete address cannot exist, so no order can hold one")
    void incompleteAddressesAreRejected() {
        assertThatThrownBy(() -> Address.of("", "Amsterdam", "1012 AB", "NL"))
                .isInstanceOf(InvalidAddressException.class);
        assertThatThrownBy(() -> Address.of("Kerkstraat 1", "Amsterdam", "1012 AB", "NLD"))
                .isInstanceOf(InvalidAddressException.class)
                .hasMessageContaining("alpha-2");
    }
}
