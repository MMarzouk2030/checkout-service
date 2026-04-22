package org.codequest.checkoutservice.shared.model.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(UUID id, Long orderId, String externalPaymentId, BigDecimal amount, String status) {
}
