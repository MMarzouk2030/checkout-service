package org.codequest.checkoutservice.order.domain;

import org.codequest.checkoutservice.order.exception.OrderException;
import org.codequest.checkoutservice.shared.model.order.OrderState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void newOrder_hasCreatedState() {
        Order order = new Order(1L, BigDecimal.TEN);
        assertThat(order.getOrderState()).isEqualTo(OrderState.CREATED);
    }

    @Test
    void transitionTo_validState_changesOrderState() {
        Order order = new Order(1L, BigDecimal.TEN);
        order.transitionTo(OrderState.PENDING_PAYMENT);
        assertThat(order.getOrderState()).isEqualTo(OrderState.PENDING_PAYMENT);
    }

    @Test
    void transitionTo_invalidState_throwsOrderException() {
        Order order = new Order(1L, BigDecimal.TEN);
        assertThatThrownBy(() -> order.transitionTo(OrderState.PAID))
                .isInstanceOf(OrderException.class);
    }

    @Test
    void addItem_appendsItemToOrder() {
        Order order = new Order(1L, BigDecimal.TEN);
        order.addItem("P1", 2, new BigDecimal("4.99"));
        assertThat(order.getItems()).hasSize(1);
    }

    @Test
    void getItems_returnsUnmodifiableList() {
        Order order = new Order(1L, BigDecimal.TEN);
        order.addItem("P1", 1, BigDecimal.TEN);
        assertThatThrownBy(() -> order.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void isInPayableState_whenCreated_returnsTrue() {
        Order order = new Order(1L, BigDecimal.TEN);
        assertThat(order.isInPayableState()).isTrue();
    }

    @Test
    void isInPayableState_whenPaymentFailed_returnsTrue() {
        Order order = new Order(1L, BigDecimal.TEN);
        order.transitionTo(OrderState.PENDING_PAYMENT);
        order.transitionTo(OrderState.PAYMENT_FAILED);
        assertThat(order.isInPayableState()).isTrue();
    }

    @Test
    void isInPayableState_whenPendingPayment_returnsFalse() {
        Order order = new Order(1L, BigDecimal.TEN);
        order.transitionTo(OrderState.PENDING_PAYMENT);
        assertThat(order.isInPayableState()).isFalse();
    }

    @Test
    void isInPayableState_whenPaid_returnsFalse() {
        Order order = new Order(1L, BigDecimal.TEN);
        order.transitionTo(OrderState.PENDING_PAYMENT);
        order.transitionTo(OrderState.PAID);
        assertThat(order.isInPayableState()).isFalse();
    }
}
