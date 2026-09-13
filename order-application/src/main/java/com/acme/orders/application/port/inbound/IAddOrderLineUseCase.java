package com.acme.orders.application.port.inbound;

import com.acme.orders.application.port.inbound.command.AddOrderLineCommand;
import com.acme.orders.application.view.OrderView;

/** Input port: puts a product on a draft order. */
public interface IAddOrderLineUseCase {

    OrderView addOrderLine(AddOrderLineCommand command);
}
