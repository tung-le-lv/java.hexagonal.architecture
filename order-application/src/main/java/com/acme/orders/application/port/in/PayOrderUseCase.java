package com.acme.orders.application.port.in;

import com.acme.orders.application.port.in.command.PayOrderCommand;
import com.acme.orders.application.view.OrderView;

/** Driving port: records confirmed payment. */
public interface PayOrderUseCase {

    OrderView payOrder(PayOrderCommand command);
}
