package org.codequest.checkoutservice.shared.rest;

import org.codequest.checkoutservice.shared.model.payment.PaymentRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/mock/payments")
public interface PaymentProviderClient {

    @PostExchange("/start")
    ResponseEntity<String> startPayment(@RequestBody PaymentRequest paymentRequest);

}
