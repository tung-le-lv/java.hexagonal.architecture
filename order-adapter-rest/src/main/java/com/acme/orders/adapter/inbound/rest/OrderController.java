package com.acme.orders.adapter.inbound.rest;

import com.acme.orders.adapter.inbound.rest.dto.AddOrderLineRequest;
import com.acme.orders.adapter.inbound.rest.dto.CancelOrderRequest;
import com.acme.orders.adapter.inbound.rest.dto.ChangeQuantityRequest;
import com.acme.orders.adapter.inbound.rest.dto.CreateOrderRequest;
import com.acme.orders.adapter.inbound.rest.dto.OrderResponse;
import com.acme.orders.adapter.inbound.rest.dto.OrderSummaryResponse;
import com.acme.orders.adapter.inbound.rest.dto.PagedResponse;
import com.acme.orders.adapter.inbound.rest.dto.PayOrderRequest;
import com.acme.orders.adapter.inbound.rest.dto.ShipOrderRequest;
import com.acme.orders.adapter.inbound.rest.mapper.OrderRestMapper;
import com.acme.orders.application.port.inbound.IAddOrderLineUseCase;
import com.acme.orders.application.port.inbound.ICancelOrderUseCase;
import com.acme.orders.application.port.inbound.IChangeOrderLineQuantityUseCase;
import com.acme.orders.application.port.inbound.ICreateDraftOrderUseCase;
import com.acme.orders.application.port.inbound.IPayOrderUseCase;
import com.acme.orders.application.port.inbound.IPlaceOrderUseCase;
import com.acme.orders.application.port.inbound.IRemoveOrderLineUseCase;
import com.acme.orders.application.port.inbound.IShipOrderUseCase;
import com.acme.orders.application.port.inbound.command.PlaceOrderCommand;
import com.acme.orders.application.port.inbound.command.RemoveOrderLineCommand;
import com.acme.orders.application.port.inbound.query.IGetOrderQuery;
import com.acme.orders.application.port.inbound.query.IListCustomerOrdersQuery;
import com.acme.orders.application.view.OrderView;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound adapter: maps HTTP onto the order use cases.
 *
 * <p>It depends on one interface per operation, so what this class is allowed to do is visible in its
 * constructor. Note what it does not contain: no business rules, no status checks, no pricing — only
 * translation in, delegation, and translation out. Each lifecycle step is modelled as a sub-resource
 * being created ({@code POST .../payment}) rather than as a status field clients may set, so the API
 * cannot express a transition the domain would reject.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final ICreateDraftOrderUseCase createDraftOrder;
    private final IAddOrderLineUseCase addOrderLine;
    private final IChangeOrderLineQuantityUseCase changeOrderLineQuantity;
    private final IRemoveOrderLineUseCase removeOrderLine;
    private final IPlaceOrderUseCase placeOrder;
    private final IPayOrderUseCase payOrder;
    private final IShipOrderUseCase shipOrder;
    private final ICancelOrderUseCase cancelOrder;
    private final IGetOrderQuery getOrder;
    private final IListCustomerOrdersQuery listCustomerOrders;

    public OrderController(ICreateDraftOrderUseCase createDraftOrder,
                           IAddOrderLineUseCase addOrderLine,
                           IChangeOrderLineQuantityUseCase changeOrderLineQuantity,
                           IRemoveOrderLineUseCase removeOrderLine,
                           IPlaceOrderUseCase placeOrder,
                           IPayOrderUseCase payOrder,
                           IShipOrderUseCase shipOrder,
                           ICancelOrderUseCase cancelOrder,
                           IGetOrderQuery getOrder,
                           IListCustomerOrdersQuery listCustomerOrders) {
        this.createDraftOrder = createDraftOrder;
        this.addOrderLine = addOrderLine;
        this.changeOrderLineQuantity = changeOrderLineQuantity;
        this.removeOrderLine = removeOrderLine;
        this.placeOrder = placeOrder;
        this.payOrder = payOrder;
        this.shipOrder = shipOrder;
        this.cancelOrder = cancelOrder;
        this.getOrder = getOrder;
        this.listCustomerOrders = listCustomerOrders;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderView view = createDraftOrder.createDraftOrder(OrderRestMapper.toCommand(request));
        return ResponseEntity
                .created(URI.create("/api/v1/orders/" + view.orderId()))
                .body(OrderRestMapper.toResponse(view));
    }

    @GetMapping("/{orderId}")
    public OrderResponse getById(@PathVariable UUID orderId) {
        return OrderRestMapper.toResponse(getOrder.byId(orderId));
    }

    @GetMapping
    public PagedResponse<OrderSummaryResponse> listByCustomer(@RequestParam UUID customerId,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return OrderRestMapper.toResponse(listCustomerOrders.forCustomer(customerId, page, size));
    }

    @PostMapping("/{orderId}/lines")
    public OrderResponse addLine(@PathVariable UUID orderId, @Valid @RequestBody AddOrderLineRequest request) {
        return OrderRestMapper.toResponse(addOrderLine.addOrderLine(OrderRestMapper.toCommand(orderId, request)));
    }

    @PatchMapping("/{orderId}/lines/{lineId}")
    public OrderResponse changeLineQuantity(@PathVariable UUID orderId,
                                            @PathVariable UUID lineId,
                                            @Valid @RequestBody ChangeQuantityRequest request) {
        return OrderRestMapper.toResponse(
                changeOrderLineQuantity.changeOrderLineQuantity(OrderRestMapper.toCommand(orderId, lineId, request)));
    }

    @DeleteMapping("/{orderId}/lines/{lineId}")
    public OrderResponse removeLine(@PathVariable UUID orderId, @PathVariable UUID lineId) {
        return OrderRestMapper.toResponse(removeOrderLine.removeOrderLine(new RemoveOrderLineCommand(orderId, lineId)));
    }

    @PostMapping("/{orderId}/placement")
    public OrderResponse place(@PathVariable UUID orderId) {
        return OrderRestMapper.toResponse(placeOrder.placeOrder(new PlaceOrderCommand(orderId)));
    }

    @PostMapping("/{orderId}/payment")
    public OrderResponse pay(@PathVariable UUID orderId, @Valid @RequestBody PayOrderRequest request) {
        return OrderRestMapper.toResponse(payOrder.payOrder(OrderRestMapper.toCommand(orderId, request)));
    }

    @PostMapping("/{orderId}/shipment")
    public OrderResponse ship(@PathVariable UUID orderId, @Valid @RequestBody ShipOrderRequest request) {
        return OrderRestMapper.toResponse(shipOrder.shipOrder(OrderRestMapper.toCommand(orderId, request)));
    }

    @PostMapping("/{orderId}/cancellation")
    public OrderResponse cancel(@PathVariable UUID orderId, @Valid @RequestBody CancelOrderRequest request) {
        return OrderRestMapper.toResponse(cancelOrder.cancelOrder(OrderRestMapper.toCommand(orderId, request)));
    }
}
