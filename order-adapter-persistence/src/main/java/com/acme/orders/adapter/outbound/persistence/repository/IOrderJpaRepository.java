package com.acme.orders.adapter.outbound.persistence.repository;

import com.acme.orders.adapter.outbound.persistence.entity.OrderJpaEntity;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data's view of the orders table.
 *
 * <p>Note that this is not the port: {@link com.acme.orders.application.port.outbound.IOrderRepository} is,
 * and the adapter implements it using this. Keeping Spring Data behind the port is what stops
 * {@code Page}, {@code Pageable} and {@code @Query} from spreading into the use cases.
 */
public interface IOrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    org.springframework.data.domain.Page<OrderJpaEntity> findAllByCustomerId(UUID customerId, Pageable pageable);
}
