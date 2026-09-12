package com.acme.orders.application.port.inbound;

import com.acme.orders.application.port.inbound.command.ChangeOrderLineQuantityCommand;
import com.acme.orders.application.view.OrderView;

/** Driving port: sets a line's quantity. */
public interface ChangeOrderLineQuantityUseCase {

    OrderView changeOrderLineQuantity(ChangeOrderLineQuantityCommand command);
}
