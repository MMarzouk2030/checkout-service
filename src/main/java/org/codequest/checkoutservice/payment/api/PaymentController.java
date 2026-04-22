package org.codequest.checkoutservice.payment.api;

import org.codequest.checkoutservice.shared.model.order.OrderState;
import org.codequest.checkoutservice.payment.domain.PaymentStatus;
import org.codequest.checkoutservice.payment.dto.WebhookRequest;
import org.codequest.checkoutservice.payment.exception.PaymentErrorCode;
import org.codequest.checkoutservice.payment.exception.PaymentException;
import org.codequest.checkoutservice.payment.service.PaymentService;
import org.codequest.checkoutservice.shared.facade.order.OrderFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderFacade orderFacade;

    public PaymentController(PaymentService paymentService, OrderFacade orderFacade) {
        this.paymentService = paymentService;
        this.orderFacade = orderFacade;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody WebhookRequest request) {
        paymentService.processWebhook(request.externalPaymentId(), request.status())
                .ifPresent(
                        payment -> orderFacade.transitOrder(payment.getOrderId(), toOrderState(payment.getStatus()))
                );
        return ResponseEntity.ok(Map.of("result", "processed"));
    }

    private OrderState toOrderState(PaymentStatus paymentStatus) {
        return switch (paymentStatus) {
            case CONFIRMED -> OrderState.PAID;
            case FAILED -> OrderState.PAYMENT_FAILED;
            default -> throw new PaymentException(PaymentErrorCode.INVALID_WEBHOOK_STATUS, paymentStatus);
        };
    }
}
