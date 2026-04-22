package org.codequest.checkoutservice.order.domain;

import org.codequest.checkoutservice.order.exception.OrderException;
import org.codequest.checkoutservice.shared.model.order.OrderState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

    // --- CREATED ---

    @Test
    void created_to_pendingPayment_isValid() {
        assertThatCode(() -> OrderStateMachine.validateTransition(OrderState.CREATED, OrderState.PENDING_PAYMENT))
                .doesNotThrowAnyException();
    }

    @Test
    void created_to_cancelled_isValid() {
        assertThatCode(() -> OrderStateMachine.validateTransition(OrderState.CREATED, OrderState.CANCELLED))
                .doesNotThrowAnyException();
    }

    @Test
    void created_to_paid_isInvalid() {
        assertThatThrownBy(() -> OrderStateMachine.validateTransition(OrderState.CREATED, OrderState.PAID))
                .isInstanceOf(OrderException.class);
    }

    @Test
    void created_to_paymentFailed_isInvalid() {
        assertThatThrownBy(() -> OrderStateMachine.validateTransition(OrderState.CREATED, OrderState.PAYMENT_FAILED))
                .isInstanceOf(OrderException.class);
    }

    // --- PENDING_PAYMENT ---

    @Test
    void pendingPayment_to_paid_isValid() {
        assertThatCode(() -> OrderStateMachine.validateTransition(OrderState.PENDING_PAYMENT, OrderState.PAID))
                .doesNotThrowAnyException();
    }

    @Test
    void pendingPayment_to_paymentFailed_isValid() {
        assertThatCode(() -> OrderStateMachine.validateTransition(OrderState.PENDING_PAYMENT, OrderState.PAYMENT_FAILED))
                .doesNotThrowAnyException();
    }

    @Test
    void pendingPayment_to_cancelled_isValid() {
        assertThatCode(() -> OrderStateMachine.validateTransition(OrderState.PENDING_PAYMENT, OrderState.CANCELLED))
                .doesNotThrowAnyException();
    }

    @Test
    void pendingPayment_to_created_isInvalid() {
        assertThatThrownBy(() -> OrderStateMachine.validateTransition(OrderState.PENDING_PAYMENT, OrderState.CREATED))
                .isInstanceOf(OrderException.class);
    }

    // --- PAYMENT_FAILED ---

    @Test
    void paymentFailed_to_pendingPayment_isValid() {
        assertThatCode(() -> OrderStateMachine.validateTransition(OrderState.PAYMENT_FAILED, OrderState.PENDING_PAYMENT))
                .doesNotThrowAnyException();
    }

    @Test
    void paymentFailed_to_cancelled_isValid() {
        assertThatCode(() -> OrderStateMachine.validateTransition(OrderState.PAYMENT_FAILED, OrderState.CANCELLED))
                .doesNotThrowAnyException();
    }

    @Test
    void paymentFailed_to_paid_isInvalid() {
        assertThatThrownBy(() -> OrderStateMachine.validateTransition(OrderState.PAYMENT_FAILED, OrderState.PAID))
                .isInstanceOf(OrderException.class);
    }

    // --- Terminal states (PAID, CANCELLED) ---

    @ParameterizedTest
    @EnumSource(OrderState.class)
    void paid_to_anyState_isInvalid(OrderState target) {
        assertThatThrownBy(() -> OrderStateMachine.validateTransition(OrderState.PAID, target))
                .isInstanceOf(OrderException.class);
    }

    @ParameterizedTest
    @EnumSource(OrderState.class)
    void cancelled_to_anyState_isInvalid(OrderState target) {
        assertThatThrownBy(() -> OrderStateMachine.validateTransition(OrderState.CANCELLED, target))
                .isInstanceOf(OrderException.class);
    }
}
