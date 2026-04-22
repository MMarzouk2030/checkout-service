package org.codequest.checkoutservice.payment.domain;

import org.codequest.checkoutservice.payment.exception.PaymentException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private Payment pending() {
        return new Payment(1L, "ext-123", BigDecimal.TEN);
    }

    @Test
    void newPayment_hasPendingStatus() {
        assertThat(pending().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void confirm_whenPending_setsConfirmed() {
        Payment payment = pending();
        payment.confirm();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    void confirm_whenAlreadyConfirmed_throwsPaymentException() {
        Payment payment = pending();
        payment.confirm();
        assertThatThrownBy(payment::confirm)
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void confirm_whenFailed_throwsPaymentException() {
        Payment payment = pending();
        payment.fail();
        assertThatThrownBy(payment::confirm)
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void fail_whenPending_setsFailed() {
        Payment payment = pending();
        payment.fail();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void fail_whenAlreadyFailed_throwsPaymentException() {
        Payment payment = pending();
        payment.fail();
        assertThatThrownBy(payment::fail)
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void fail_whenConfirmed_throwsPaymentException() {
        Payment payment = pending();
        payment.confirm();
        assertThatThrownBy(payment::fail)
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void isTerminal_whenPending_returnsFalse() {
        assertThat(pending().isTerminal()).isFalse();
    }

    @Test
    void isTerminal_whenConfirmed_returnsTrue() {
        Payment payment = pending();
        payment.confirm();
        assertThat(payment.isTerminal()).isTrue();
    }

    @Test
    void isTerminal_whenFailed_returnsTrue() {
        Payment payment = pending();
        payment.fail();
        assertThat(payment.isTerminal()).isTrue();
    }
}
