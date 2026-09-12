package com.acme.orders.application.port.inbound.query;

import com.acme.orders.application.view.OrderView;
import java.util.UUID;

/**
 * Driving port for reads, kept separate from the command ports.
 *
 * <p>Reads need no aggregate, no transaction boundary and no events, so routing them through the
 * same interfaces as writes would only force every caller to pay for machinery it does not use.
 */
public interface GetOrderQuery {

    OrderView byId(UUID orderId);
}
