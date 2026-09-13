package com.acme.orders.application.port.inbound;

import com.acme.orders.application.port.inbound.command.PlaceOrderCommand;
import com.acme.orders.application.view.OrderView;

/** Input port: submits a draft order, pricing it with the discount policy in force. */
public interface IPlaceOrderUseCase {

    OrderView placeOrder(PlaceOrderCommand command);
}
