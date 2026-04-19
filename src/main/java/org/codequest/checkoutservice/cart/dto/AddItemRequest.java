package org.codequest.checkoutservice.cart.dto;

import java.math.BigDecimal;

public record AddItemRequest(String productId, int quantity, BigDecimal price) {
}
