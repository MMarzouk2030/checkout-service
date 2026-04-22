package org.codequest.checkoutservice.payment.service;

import org.codequest.checkoutservice.payment.domain.Payment;
import org.codequest.checkoutservice.payment.domain.PaymentStatus;
import org.codequest.checkoutservice.payment.exception.PaymentException;
import org.codequest.checkoutservice.payment.repository.PaymentRepository;
import org.codequest.checkoutservice.shared.exception.ResourceNotFoundException;
import org.codequest.checkoutservice.shared.rest.PaymentProviderClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;
    @Mock
    PaymentProviderClient paymentProviderClient;

    @InjectMocks
    PaymentService paymentService;

    // --- startPayment ---

    @Test
    void startPayment_whenNoDuplicateActive_createsAndSavesPayment() {
        when(paymentRepository.existsByOrderIdAndStatus(1L, PaymentStatus.PENDING)).thenReturn(false);
        when(paymentProviderClient.startPayment(any())).thenReturn(ResponseEntity.ok("ext-123"));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.startPayment(1L, BigDecimal.TEN);

        assertThat(result.getExternalPaymentId()).isEqualTo("ext-123");
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getOrderId()).isEqualTo(1L);
    }

    @Test
    void startPayment_whenDuplicateActivePending_throwsPaymentException() {
        when(paymentRepository.existsByOrderIdAndStatus(1L, PaymentStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.startPayment(1L, BigDecimal.TEN))
                .isInstanceOf(PaymentException.class);

        verifyNoInteractions(paymentProviderClient);
        verify(paymentRepository, never()).save(any());
    }

    // --- processWebhook (CONFIRMED) ---

    @Test
    void processWebhook_confirmedStatus_confirmsPaymentAndReturnsIt() {
        Payment payment = new Payment(1L, "ext-123", BigDecimal.TEN);
        when(paymentRepository.findByExternalPaymentId("ext-123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<Payment> result = paymentService.processWebhook("ext-123", "CONFIRMED");

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    }

    // --- processWebhook (FAILED) ---

    @Test
    void processWebhook_failedStatus_failsPaymentAndReturnsIt() {
        Payment payment = new Payment(1L, "ext-123", BigDecimal.TEN);
        when(paymentRepository.findByExternalPaymentId("ext-123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<Payment> result = paymentService.processWebhook("ext-123", "FAILED");

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    // --- Idempotency: duplicate webhook ---

    @Test
    void processWebhook_whenAlreadyConfirmed_returnsEmptyWithoutSaving() {
        Payment payment = new Payment(1L, "ext-123", BigDecimal.TEN);
        payment.confirm();
        when(paymentRepository.findByExternalPaymentId("ext-123")).thenReturn(Optional.of(payment));

        Optional<Payment> result = paymentService.processWebhook("ext-123", "CONFIRMED");

        assertThat(result).isEmpty();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processWebhook_whenAlreadyFailed_returnsEmptyWithoutSaving() {
        Payment payment = new Payment(1L, "ext-123", BigDecimal.TEN);
        payment.fail();
        when(paymentRepository.findByExternalPaymentId("ext-123")).thenReturn(Optional.of(payment));

        Optional<Payment> result = paymentService.processWebhook("ext-123", "FAILED");

        assertThat(result).isEmpty();
        verify(paymentRepository, never()).save(any());
    }

    // --- Conflicting update on terminal payment ---

    @Test
    void processWebhook_whenTerminalWithDifferentStatus_throwsPaymentException() {
        Payment payment = new Payment(1L, "ext-123", BigDecimal.TEN);
        payment.confirm();
        when(paymentRepository.findByExternalPaymentId("ext-123")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.processWebhook("ext-123", "FAILED"))
                .isInstanceOf(PaymentException.class);

        verify(paymentRepository, never()).save(any());
    }

    // --- Payment not found ---

    @Test
    void processWebhook_whenPaymentNotFound_throwsResourceNotFoundException() {
        when(paymentRepository.findByExternalPaymentId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processWebhook("unknown", "CONFIRMED"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Invalid status ---

    @Test
    void processWebhook_whenInvalidStatus_throwsIllegalArgumentException() {
        Payment payment = new Payment(1L, "ext-123", BigDecimal.TEN);
        when(paymentRepository.findByExternalPaymentId("ext-123")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.processWebhook("ext-123", "NOT_A_STATUS"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- PENDING status in webhook is treated as idempotent no-op ---

    @Test
    void processWebhook_whenStatusIsPendingAndPaymentAlreadyPending_returnsEmpty() {
        // A webhook with PENDING status on an already-PENDING payment is a no-op (idempotency guard).
        Payment payment = new Payment(1L, "ext-123", BigDecimal.TEN);
        when(paymentRepository.findByExternalPaymentId("ext-123")).thenReturn(Optional.of(payment));

        Optional<Payment> result = paymentService.processWebhook("ext-123", "PENDING");

        assertThat(result).isEmpty();
        verify(paymentRepository, never()).save(any());
    }
}
