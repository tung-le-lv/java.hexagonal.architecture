package com.acme.orders.application.port.in;

import com.acme.orders.application.port.in.command.RemoveOrderLineCommand;
import com.acme.orders.application.view.OrderView;

/** Driving port: removes a line from a draft order. */
public interface RemoveOrderLineUseCase {

    OrderView removeOrderLine(RemoveOrderLineCommand command);
}
