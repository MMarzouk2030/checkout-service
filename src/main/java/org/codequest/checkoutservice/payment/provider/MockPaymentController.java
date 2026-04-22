package org.codequest.checkoutservice.payment.provider;

import org.codequest.checkoutservice.shared.model.payment.PaymentRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Simulates an external payment provider.
 * Use these endpoints to trigger payment confirmation or failure.
 */
@Profile("local")
@RestController
@RequestMapping("/mock/payments")
public class MockPaymentController {

    private final MockPaymentProviderService mockPaymentProviderService;

    public MockPaymentController(MockPaymentProviderService mockPaymentProviderService) {
        this.mockPaymentProviderService = mockPaymentProviderService;
    }

    @PostMapping("/start")
    public ResponseEntity<String> startPayment(@RequestBody PaymentRequest paymentRequest) {
        String externalPaymentId = mockPaymentProviderService.createPaymentIntent(paymentRequest);
        return ResponseEntity.ok(externalPaymentId);
    }

    @PutMapping("/{externalPaymentId}/confirm")
    public ResponseEntity<Map<String, String>> confirmPayment(@PathVariable String externalPaymentId) {
        mockPaymentProviderService.callWebhook(externalPaymentId, "CONFIRMED");
        return ResponseEntity.ok(Map.of("result", "confirmed"));
    }

    @PutMapping("/{externalPaymentId}/fail")
    public ResponseEntity<Map<String, String>> failPayment(@PathVariable String externalPaymentId) {
        mockPaymentProviderService.callWebhook(externalPaymentId, "FAILED");
        return ResponseEntity.ok(Map.of("result", "failed"));
    }
}
