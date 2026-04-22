package org.codequest.checkoutservice.shared.model.payment;

import java.math.BigDecimal;

public record PaymentRequest(Long orderId, BigDecimal amount) {
}
