package com.acme.orders.adapter.outbound.persistence.repository;

import com.acme.orders.adapter.outbound.persistence.entity.OutboxMessageJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to the outbox table. */
public interface IOutboxJpaRepository extends JpaRepository<OutboxMessageJpaEntity, UUID> {

    /** Unpublished messages, oldest first, so events reach the broker in the order they happened. */
    List<OutboxMessageJpaEntity> findByPublishedAtIsNullOrderByOccurredAtAsc(Pageable pageable);

    long countByPublishedAtIsNull();
}
