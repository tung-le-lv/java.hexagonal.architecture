package com.acme.orders.adapter.outbound.persistence;

import com.acme.orders.adapter.outbound.persistence.entity.OrderJpaEntity;
import com.acme.orders.adapter.outbound.persistence.mapper.OrderPersistenceMapper;
import com.acme.orders.adapter.outbound.persistence.repository.IOrderJpaRepository;
import com.acme.orders.application.exception.ConcurrentModificationException;
import com.acme.orders.application.port.outbound.IOrderQueryRepository;
import com.acme.orders.application.view.OrderSummaryView;
import com.acme.orders.application.view.OrderView;
import com.acme.orders.application.view.Page;
import com.acme.orders.domain.repository.IOrderRepository;
import com.acme.orders.domain.aggregate.Order;
import com.acme.orders.domain.valueobject.OrderId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Driven adapter implementing both persistence ports over JPA.
 *
 * <p>It also translates the data-access layer's failures into the application's vocabulary — a
 * Spring {@link OptimisticLockingFailureException} becomes a
 * {@link ConcurrentModificationException} — so that a leaked framework exception cannot become part
 * of the core's de facto contract.
 */
@Component
public class OrderPersistenceAdapter implements IOrderRepository, IOrderQueryRepository {

    private final IOrderJpaRepository orders;

    public OrderPersistenceAdapter(IOrderJpaRepository orders) {
        this.orders = orders;
    }

    // ------------------------------------------------------------------ write side

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(OrderId orderId) {
        return orders.findById(orderId.value()).map(OrderPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public Order save(Order order) {
        UUID id = order.id().value();
        try {
            OrderJpaEntity managed = orders.findById(id).orElse(null);
            if (managed == null) {
                OrderJpaEntity inserted = orders.saveAndFlush(OrderPersistenceMapper.toNewEntity(order));
                return OrderPersistenceMapper.toDomain(inserted);
            }

            // The aggregate may have been loaded in an earlier transaction. JPA's own version check
            // only covers changes made after this transaction read the row, so compare explicitly and
            // refuse to overwrite work this caller never saw.
            if (order.version() != managed.versionOrZero()) {
                throw new ConcurrentModificationException(id, null);
            }

            OrderPersistenceMapper.updateEntity(managed, order);
            OrderJpaEntity updated = orders.saveAndFlush(managed);
            return OrderPersistenceMapper.toDomain(updated);
        } catch (OptimisticLockingFailureException cause) {
            throw new ConcurrentModificationException(id, cause);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(OrderId orderId) {
        return orders.existsById(orderId.value());
    }

    // ------------------------------------------------------------------ read side

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderView> findById(UUID orderId) {
        return orders.findById(orderId).map(OrderPersistenceMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryView> findByCustomerId(UUID customerId, int pageNumber, int pageSize) {
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<OrderJpaEntity> page =
                orders.findAllByCustomerId(customerId, pageRequest);

        // Spring Data's Page stops here; the port returns the application's own Page type.
        return Page.of(
                page.getContent().stream().map(OrderPersistenceMapper::toSummaryView).toList(),
                pageNumber,
                pageSize,
                page.getTotalElements());
    }
}
