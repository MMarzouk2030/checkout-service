package org.codequest.checkoutservice.payment.service;

import org.codequest.checkoutservice.payment.domain.Payment;
import org.codequest.checkoutservice.payment.domain.PaymentStatus;
import org.codequest.checkoutservice.payment.exception.PaymentErrorCode;
import org.codequest.checkoutservice.payment.exception.PaymentException;
import org.codequest.checkoutservice.payment.repository.PaymentRepository;
import org.codequest.checkoutservice.shared.exception.ErrorCode;
import org.codequest.checkoutservice.shared.exception.ResourceNotFoundException;
import org.codequest.checkoutservice.shared.model.payment.PaymentRequest;
import org.codequest.checkoutservice.shared.rest.PaymentProviderClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProviderClient paymentProviderClient;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentProviderClient paymentProviderClient) {
        this.paymentRepository = paymentRepository;
        this.paymentProviderClient = paymentProviderClient;
    }

    @Transactional
    public Payment startPayment(Long orderId, BigDecimal orderTotalAmount) {
        // [idempotency] Prevent duplicate active payments
        if (paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.PENDING)) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        PaymentRequest paymentRequest = new PaymentRequest(orderId, orderTotalAmount);
        String externalPaymentId = paymentProviderClient.startPayment(paymentRequest).getBody();

        Payment payment = new Payment(orderId, externalPaymentId, orderTotalAmount);
        payment = paymentRepository.save(payment);

        return payment;
    }

    @Transactional
    public Optional<Payment> processWebhook(String externalPaymentId, String status) {
        Payment payment = paymentRepository.findByExternalPaymentId(externalPaymentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PAYMENT_NOT_FOUND));

        PaymentStatus targetStatus = PaymentStatus.valueOf(status);

        // Idempotency: already in the target state → no-op, caller needs no further action
        if (payment.getStatus().equals(targetStatus)) {
            return Optional.empty();
        }

        // Reject conflicting updates to a terminal payment
        if (payment.isTerminal()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_COMPLETED, payment.getStatus());
        }

        if (targetStatus.equals(PaymentStatus.CONFIRMED)) {
            payment.confirm();
        } else if (targetStatus.equals(PaymentStatus.FAILED)) {
            payment.fail();
        } else {
            throw new PaymentException(PaymentErrorCode.INVALID_WEBHOOK_STATUS, payment.getStatus());
        }

        return Optional.of(paymentRepository.save(payment));
    }
}
