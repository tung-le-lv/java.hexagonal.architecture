package com.acme.orders.application.port.inbound;

import com.acme.orders.application.port.inbound.command.PlaceOrderCommand;
import com.acme.orders.application.view.OrderView;

/** Driving port: submits a draft order, pricing it with the discount policy in force. */
public interface PlaceOrderUseCase {

    OrderView placeOrder(PlaceOrderCommand command);
}
