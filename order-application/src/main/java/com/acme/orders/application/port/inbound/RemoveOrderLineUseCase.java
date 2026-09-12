package com.acme.orders.application.port.inbound;

import com.acme.orders.application.port.inbound.command.RemoveOrderLineCommand;
import com.acme.orders.application.view.OrderView;

/** Driving port: removes a line from a draft order. */
public interface RemoveOrderLineUseCase {

    OrderView removeOrderLine(RemoveOrderLineCommand command);
}
