package com.acme.orders.adapter.in.rest.dto;

import java.util.List;

/** Envelope for paginated collections, so clients get paging metadata alongside the rows. */
public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {
}
