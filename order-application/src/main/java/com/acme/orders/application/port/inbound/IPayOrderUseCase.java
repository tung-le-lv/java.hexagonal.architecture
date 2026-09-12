package com.acme.orders.application.port.inbound;

import com.acme.orders.application.port.inbound.command.PayOrderCommand;
import com.acme.orders.application.view.OrderView;

/** Driving port: records confirmed payment. */
public interface IPayOrderUseCase {

    OrderView payOrder(PayOrderCommand command);
}
