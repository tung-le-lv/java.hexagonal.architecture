package com.acme.orders.application.port.inbound;

import com.acme.orders.application.port.inbound.command.CancelOrderCommand;
import com.acme.orders.application.view.OrderView;

/** Driving port: abandons an order that has not shipped. */
public interface CancelOrderUseCase {

    OrderView cancelOrder(CancelOrderCommand command);
}
