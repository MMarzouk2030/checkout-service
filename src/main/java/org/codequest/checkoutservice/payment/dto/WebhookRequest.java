package org.codequest.checkoutservice.payment.dto;

public record WebhookRequest(String externalPaymentId, String status) {
}
