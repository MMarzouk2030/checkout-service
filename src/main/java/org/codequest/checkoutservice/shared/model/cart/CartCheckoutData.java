package org.codequest.checkoutservice.shared.model.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartCheckoutData(
        Long cartId,
        List<CartItemData> items,
        BigDecimal totalAmount
) {
    public record CartItemData(
            String productId,
            int quantity,
            BigDecimal price
    ) {
    }
}
