package org.codequest.checkoutservice.payment.api;

import org.codequest.checkoutservice.payment.application.PaymentWebhookApplicationService;
import org.codequest.checkoutservice.payment.dto.WebhookRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentWebhookApplicationService webhookApplicationService;

    public PaymentController(PaymentWebhookApplicationService webhookApplicationService) {
        this.webhookApplicationService = webhookApplicationService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody WebhookRequest request) {
        log.info("POST /payments/webhook [externalPaymentId={}, status={}]",
                request.externalPaymentId(), request.status());
        webhookApplicationService.processWebhook(request.externalPaymentId(), request.status());
        return ResponseEntity.ok(Map.of("result", "processed"));
    }
}
