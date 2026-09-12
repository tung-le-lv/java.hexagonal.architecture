package com.acme.orders.domain.exception;

/** Raised when a transition is attempted without a detail the business requires (a reference, a reason). */
public class MissingOrderDetailException extends DomainException {

    public MissingOrderDetailException(String message) {
        super("order.detail_missing", message);
    }
}
