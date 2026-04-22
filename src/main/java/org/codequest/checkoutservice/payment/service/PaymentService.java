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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentProviderClient paymentProviderClient;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentProviderClient paymentProviderClient) {
        this.paymentRepository = paymentRepository;
        this.paymentProviderClient = paymentProviderClient;
    }

    @Transactional
    public Payment startPayment(Long orderId, BigDecimal orderTotalAmount) {
        if (paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.PENDING)) {
            log.warn("Duplicate payment attempt rejected — active payment already exists [orderId={}]", orderId);
            throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        log.info("Requesting payment from provider [orderId={}, amount={}]", orderId, orderTotalAmount);
        PaymentRequest paymentRequest = new PaymentRequest(orderId, orderTotalAmount);
        String externalPaymentId = paymentProviderClient.startPayment(paymentRequest).getBody();

        Payment payment = new Payment(orderId, externalPaymentId, orderTotalAmount);
        payment = paymentRepository.save(payment);

        log.info("Payment created [orderId={}, paymentId={}, externalPaymentId={}]",
                orderId, payment.getId(), externalPaymentId);
        return payment;
    }

    @Transactional
    public Optional<Payment> processWebhook(String externalPaymentId, String status) {
        log.info("Webhook received [externalPaymentId={}, status={}]", externalPaymentId, status);

        Payment payment = paymentRepository.findByExternalPaymentId(externalPaymentId)
                .orElseThrow(() -> {
                    log.warn("Webhook for unknown payment [externalPaymentId={}]", externalPaymentId);
                    return new ResourceNotFoundException(ErrorCode.PAYMENT_NOT_FOUND);
                });

        PaymentStatus targetStatus = PaymentStatus.valueOf(status);

        if (payment.getStatus().equals(targetStatus)) {
            log.info("Idempotent webhook — payment already in target state [externalPaymentId={}, status={}]",
                    externalPaymentId, targetStatus);
            return Optional.empty();
        }

        if (payment.isTerminal()) {
            log.warn("Conflicting webhook — payment already terminal [externalPaymentId={}, currentStatus={}, incomingStatus={}]",
                    externalPaymentId, payment.getStatus(), targetStatus);
            throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_COMPLETED, payment.getStatus());
        }

        if (targetStatus.equals(PaymentStatus.CONFIRMED)) {
            payment.confirm();
            log.info("Payment confirmed [externalPaymentId={}, orderId={}]",
                    externalPaymentId, payment.getOrderId());
        } else if (targetStatus.equals(PaymentStatus.FAILED)) {
            payment.fail();
            log.info("Payment failed [externalPaymentId={}, orderId={}]",
                    externalPaymentId, payment.getOrderId());
        } else {
            log.warn("Invalid webhook status [externalPaymentId={}, status={}]", externalPaymentId, status);
            throw new PaymentException(PaymentErrorCode.INVALID_WEBHOOK_STATUS, payment.getStatus());
        }

        return Optional.of(paymentRepository.save(payment));
    }
}
