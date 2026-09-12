package com.acme.orders.application.view;

import java.util.List;

/**
 * A slice of results, owned by the application rather than borrowed from a persistence framework.
 *
 * <p>Using Spring Data's {@code Page} here would drag the data-access library through every port and
 * into the inbound adapter, which is exactly the coupling the hexagon exists to prevent.
 */
public record Page<T>(List<T> content, int pageNumber, int pageSize, long totalElements) {

    public Page {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public int totalPages() {
        return pageSize == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
    }

    public boolean hasNext() {
        return (long) (pageNumber + 1) * pageSize < totalElements;
    }

    public static <T> Page<T> of(List<T> content, int pageNumber, int pageSize, long totalElements) {
        return new Page<>(content, pageNumber, pageSize, totalElements);
    }
}
