package com.acme.orders.application.port.inbound;

import com.acme.orders.application.port.inbound.command.ShipOrderCommand;
import com.acme.orders.application.view.OrderView;

/** Driving port: records dispatch to the carrier. */
public interface ShipOrderUseCase {

    OrderView shipOrder(ShipOrderCommand command);
}
