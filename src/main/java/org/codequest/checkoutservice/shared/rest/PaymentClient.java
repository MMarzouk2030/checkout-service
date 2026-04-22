package org.codequest.checkoutservice.shared.rest;

import org.codequest.checkoutservice.payment.dto.WebhookRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

@HttpExchange("/payments")
public interface PaymentClient {

    @PostExchange("/webhook")
    ResponseEntity<Map<String, String>> handleWebhook(@RequestBody WebhookRequest request);

}
