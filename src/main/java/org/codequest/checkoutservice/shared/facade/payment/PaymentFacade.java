package org.codequest.checkoutservice.shared.facade.payment;

import org.codequest.checkoutservice.shared.model.payment.PaymentResponse;

import java.math.BigDecimal;

public interface PaymentFacade {

    PaymentResponse startPayment(Long orderId, BigDecimal orderTotalAmount);

}
