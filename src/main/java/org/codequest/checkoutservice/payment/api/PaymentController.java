package org.codequest.checkoutservice.payment.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Payment webhook", description = "Callback endpoint for the external payment provider")
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentWebhookApplicationService webhookApplicationService;

    public PaymentController(PaymentWebhookApplicationService webhookApplicationService) {
        this.webhookApplicationService = webhookApplicationService;
    }

    @Operation(summary = "Receive a payment status webhook",
               description = """
                       Called by the payment provider when a payment is confirmed or failed.

                       - **Idempotent** — delivering the same status twice is a no-op (HTTP 200, no state change).
                       - **CONFIRMED** → transitions Order to `PAID`.
                       - **FAILED** → transitions Order to `PAYMENT_FAILED`; client may retry via `/orders/{id}/payment/start`.
                       - Returns HTTP 404 if `externalPaymentId` is unknown.
                       - Returns HTTP 400 if the payment is already in a terminal state with a *different* status.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook processed (or duplicate — no-op)"),
            @ApiResponse(responseCode = "400", description = "Payment already completed with a different status"),
            @ApiResponse(responseCode = "404", description = "Payment not found for the given externalPaymentId")
    })
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestBody WebhookRequest request) {
        log.info("POST /payments/webhook [externalPaymentId={}, status={}]",
                request.externalPaymentId(), request.status());
        webhookApplicationService.processWebhook(request.externalPaymentId(), request.status());
        return ResponseEntity.ok(Map.of("result", "processed"));
    }
}
