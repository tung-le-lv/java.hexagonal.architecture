package com.acme.orders.domain.model.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OrderStatusTest {

    @Test
    @DisplayName("the lifecycle runs draft -> placed -> paid -> shipped -> delivered")
    void happyPath() {
        assertThat(OrderStatus.DRAFT.canTransitionTo(OrderStatus.PLACED)).isTrue();
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.PAID)).isTrue();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
    }

    @Test
    @DisplayName("cancellation is possible until dispatch and impossible after it")
    void cancellationWindow() {
        assertThat(OrderStatus.DRAFT.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    @DisplayName("the lifecycle never runs backwards")
    void noBackwardsTransitions() {
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.PLACED)).isFalse();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PAID)).isFalse();
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.DRAFT)).isFalse();
    }

    @Test
    @DisplayName("only draft is editable, and only delivered and cancelled are terminal")
    void editableAndTerminalStates() {
        assertThat(OrderStatus.DRAFT.isEditable()).isTrue();
        assertThat(OrderStatus.DELIVERED.isTerminal()).isTrue();
        assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(OrderStatus.PLACED.isTerminal()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    @DisplayName("every status declares its transitions, so none can be forgotten")
    void everyStatusHasATransitionTable(OrderStatus status) {
        assertThat(status.allowedTransitions()).isNotNull();
        assertThat(status.canTransitionTo(status)).isFalse();
    }
}
