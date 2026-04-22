package org.codequest.checkoutservice.payment.provider;

import org.codequest.checkoutservice.payment.dto.WebhookRequest;
import org.codequest.checkoutservice.shared.exception.ErrorCode;
import org.codequest.checkoutservice.shared.exception.ResourceNotFoundException;
import org.codequest.checkoutservice.shared.model.payment.PaymentRequest;
import org.codequest.checkoutservice.shared.rest.PaymentClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Profile("local")
@Service
public class MockPaymentProviderService {

    public MockPaymentProviderService(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    public record PaymentIntent(String externalPaymentId, Long orderId, LocalDateTime paymentTime, BigDecimal amount) {
    }

    private final ConcurrentMap<String, PaymentIntent> intents = new ConcurrentHashMap<>();
    private final PaymentClient paymentClient;

    public String createPaymentIntent(PaymentRequest paymentRequest) {
        String externalId = "p_" + UUID.randomUUID().toString().replace("-", "");
        intents.put(externalId, new PaymentIntent(externalId, paymentRequest.orderId(), LocalDateTime.now(), paymentRequest.amount()));
        return externalId;
    }

    public void callWebhook(String externalPaymentId, String paymentStatus) {
        validateIntentExistence(externalPaymentId);

        WebhookRequest webhookRequest = new WebhookRequest(externalPaymentId, paymentStatus);
        paymentClient.handleWebhook(webhookRequest);
    }

    private void validateIntentExistence(String externalPaymentId) {
        PaymentIntent intent = intents.get(externalPaymentId);
        if (intent == null) {
            throw new ResourceNotFoundException(ErrorCode.PAYMENT_INTENT_NOT_FOUND);
        }
    }

}
