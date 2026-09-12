package com.acme.orders.application.port.in;

import com.acme.orders.application.port.in.command.AddOrderLineCommand;
import com.acme.orders.application.view.OrderView;

/** Driving port: puts a product on a draft order. */
public interface AddOrderLineUseCase {

    OrderView addOrderLine(AddOrderLineCommand command);
}
