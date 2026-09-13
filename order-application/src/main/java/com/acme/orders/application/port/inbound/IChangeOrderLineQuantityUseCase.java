package com.acme.orders.application.port.inbound;

import com.acme.orders.application.port.inbound.command.ChangeOrderLineQuantityCommand;
import com.acme.orders.application.view.OrderView;

/** Input port: sets a line's quantity. */
public interface IChangeOrderLineQuantityUseCase {

    OrderView changeOrderLineQuantity(ChangeOrderLineQuantityCommand command);
}
