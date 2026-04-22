package org.codequest.checkoutservice.payment.application;

import org.codequest.checkoutservice.payment.domain.Payment;
import org.codequest.checkoutservice.payment.exception.PaymentException;
import org.codequest.checkoutservice.payment.service.PaymentService;
import org.codequest.checkoutservice.shared.facade.order.OrderFacade;
import org.codequest.checkoutservice.shared.model.order.OrderState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookApplicationServiceTest {

    @Mock
    PaymentService paymentService;
    @Mock
    OrderFacade orderFacade;

    @InjectMocks
    PaymentWebhookApplicationService service;

    /**
     * CONFIRMED → order transitioned to PAID
     */
    @Test
    void processWebhook_confirmedStatus_transitionsOrderToPaid() {
        Payment payment = confirmedPayment();
        when(paymentService.processWebhook("ext-1", "CONFIRMED")).thenReturn(Optional.of(payment));

        service.processWebhook("ext-1", "CONFIRMED");

        verify(orderFacade).transitOrder(42L, OrderState.PAID);
    }

    /**
     * FAILED → order transitioned to PAYMENT_FAILED
     */
    @Test
    void processWebhook_failedStatus_transitionsOrderToPaymentFailed() {
        Payment payment = failedPayment();
        when(paymentService.processWebhook("ext-2", "FAILED")).thenReturn(Optional.of(payment));

        service.processWebhook("ext-2", "FAILED");

        verify(orderFacade).transitOrder(7L, OrderState.PAYMENT_FAILED);
    }

    /**
     * Empty Optional → idempotent no-op (duplicate webhook delivery)
     */
    @Test
    void processWebhook_emptyOptional_doesNotTransitionOrder() {
        when(paymentService.processWebhook("ext-dup", "CONFIRMED")).thenReturn(Optional.empty());

        service.processWebhook("ext-dup", "CONFIRMED");

        verifyNoInteractions(orderFacade);
    }

    /**
     * Unhandled PaymentStatus (PENDING) → throws PaymentException
     */
    @Test
    void processWebhook_unhandledPaymentStatus_throwsPaymentException() {
        // A PENDING payment — toOrderState() has no case for it → default throws
        Payment pendingPayment = new Payment(99L, "ext-pending", BigDecimal.TEN);
        when(paymentService.processWebhook("ext-pending", "PENDING")).thenReturn(Optional.of(pendingPayment));

        assertThatThrownBy(() -> service.processWebhook("ext-pending", "PENDING"))
                .isInstanceOf(PaymentException.class);

        verifyNoInteractions(orderFacade);
    }

    /* Helpers */
    private Payment confirmedPayment() {
        Payment p = new Payment(42L, "ext-c", BigDecimal.TEN);
        p.confirm();
        return p;
    }

    private Payment failedPayment() {
        Payment p = new Payment(7L, "ext-f", BigDecimal.TEN);
        p.fail();
        return p;
    }
}
