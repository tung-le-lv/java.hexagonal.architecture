package com.acme.orders.domain.model.shared;

import com.acme.orders.domain.exception.InvalidAddressException;

/** A postal address. Validated on construction so no order can ever hold an unshippable address. */
public record Address(String street, String city, String postalCode, String countryCode) {

    public Address {
        street = requireText(street, "street");
        city = requireText(city, "city");
        postalCode = requireText(postalCode, "postalCode");
        countryCode = requireText(countryCode, "countryCode").toUpperCase();
        if (countryCode.length() != 2) {
            throw new InvalidAddressException("countryCode must be an ISO 3166-1 alpha-2 code but was " + countryCode);
        }
    }

    public static Address of(String street, String city, String postalCode, String countryCode) {
        return new Address(street, city, postalCode, countryCode);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidAddressException(field + " must not be blank");
        }
        return value.strip();
    }

    @Override
    public String toString() {
        return street + ", " + postalCode + " " + city + ", " + countryCode;
    }
}
