package org.codequest.checkoutservice.payment.application;

import org.codequest.checkoutservice.payment.domain.PaymentStatus;
import org.codequest.checkoutservice.payment.exception.PaymentErrorCode;
import org.codequest.checkoutservice.payment.exception.PaymentException;
import org.codequest.checkoutservice.payment.service.PaymentService;
import org.codequest.checkoutservice.shared.facade.order.OrderFacade;
import org.codequest.checkoutservice.shared.model.order.OrderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWebhookApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookApplicationService.class);

    private final PaymentService paymentService;
    private final OrderFacade orderFacade;

    public PaymentWebhookApplicationService(PaymentService paymentService, OrderFacade orderFacade) {
        this.paymentService = paymentService;
        this.orderFacade = orderFacade;
    }

    @Transactional
    public void processWebhook(String externalPaymentId, String status) {
        paymentService.processWebhook(externalPaymentId, status)
                .ifPresent(payment -> {
                    OrderState orderState = toOrderState(payment.getStatus());
                    log.info("Transitioning order after webhook [externalPaymentId={}, orderId={}, newOrderState={}]",
                            externalPaymentId, payment.getOrderId(), orderState);
                    orderFacade.transitOrder(payment.getOrderId(), orderState);
                });
    }

    private OrderState toOrderState(PaymentStatus paymentStatus) {
        return switch (paymentStatus) {
            case CONFIRMED -> OrderState.PAID;
            case FAILED -> OrderState.PAYMENT_FAILED;
            default -> throw new PaymentException(PaymentErrorCode.INVALID_WEBHOOK_STATUS, paymentStatus);
        };
    }
}
