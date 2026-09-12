package com.acme.orders.application.port.inbound;

import com.acme.orders.application.port.inbound.command.CreateDraftOrderCommand;
import com.acme.orders.application.view.OrderView;

/**
 * Driving port: starts a new draft order.
 *
 * <p>One interface per use case rather than one wide "OrderService". An inbound adapter then depends
 * only on the operation it actually drives, and a test double only has to fake that one method.
 */
public interface CreateDraftOrderUseCase {

    OrderView createDraftOrder(CreateDraftOrderCommand command);
}
