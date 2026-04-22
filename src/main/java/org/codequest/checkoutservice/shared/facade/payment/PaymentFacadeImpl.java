package org.codequest.checkoutservice.shared.facade.payment;

import org.codequest.checkoutservice.payment.domain.Payment;
import org.codequest.checkoutservice.payment.service.PaymentService;
import org.codequest.checkoutservice.shared.model.payment.PaymentResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentFacadeImpl implements PaymentFacade {

    private final PaymentService paymentService;

    public PaymentFacadeImpl(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public PaymentResponse startPayment(Long orderId, BigDecimal orderTotalAmount) {
        Payment payment = paymentService.startPayment(orderId, orderTotalAmount);
        return from(payment);
    }

    private PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getExternalPaymentId(),
                payment.getAmount(),
                payment.getStatus().name()
        );
    }

}
