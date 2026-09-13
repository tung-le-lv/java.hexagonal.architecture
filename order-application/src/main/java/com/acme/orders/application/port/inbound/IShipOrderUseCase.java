package com.acme.orders.application.port.inbound;

import com.acme.orders.application.port.inbound.command.ShipOrderCommand;
import com.acme.orders.application.view.OrderView;

/** Input port: records dispatch to the carrier. */
public interface IShipOrderUseCase {

    OrderView shipOrder(ShipOrderCommand command);
}
