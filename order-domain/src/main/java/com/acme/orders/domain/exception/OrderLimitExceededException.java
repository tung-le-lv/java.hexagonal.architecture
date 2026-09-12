package com.acme.orders.domain.exception;

/** Raised when an order would exceed a structural limit of the aggregate. */
public class OrderLimitExceededException extends DomainException {

    public OrderLimitExceededException(String message) {
        super("order.limit_exceeded", message);
    }
}
