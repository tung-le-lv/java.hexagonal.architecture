package com.acme.orders.application.port.in;

import com.acme.orders.application.port.in.command.CancelOrderCommand;
import com.acme.orders.application.view.OrderView;

/** Driving port: abandons an order that has not shipped. */
public interface CancelOrderUseCase {

    OrderView cancelOrder(CancelOrderCommand command);
}
