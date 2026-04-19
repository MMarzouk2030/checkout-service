package org.codequest.checkoutservice.cart.dto;

import org.codequest.checkoutservice.cart.domain.Cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Long id, String status, List<CartItemResponse> items, BigDecimal total) {

    public record CartItemResponse(Long id, String productId, int quantity, BigDecimal price) {
    }

    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(i -> new CartItemResponse(i.getId(), i.getProductId(), i.getQuantity(), i.getPrice()))
                .toList();
        return new CartResponse(cart.getId(), cart.getStatus().name(), items, cart.getTotal());
    }
}
