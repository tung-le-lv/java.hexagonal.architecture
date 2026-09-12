package com.acme.orders.application.port.in;

import com.acme.orders.application.port.in.command.ChangeOrderLineQuantityCommand;
import com.acme.orders.application.view.OrderView;

/** Driving port: sets a line's quantity. */
public interface ChangeOrderLineQuantityUseCase {

    OrderView changeOrderLineQuantity(ChangeOrderLineQuantityCommand command);
}
