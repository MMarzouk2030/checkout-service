package org.codequest.checkoutservice.shared.model.order;

import org.codequest.checkoutservice.order.domain.Order;

import java.math.BigDecimal;
import java.util.List;

public record OrderSummary(
        Long orderId,
        String state,
        BigDecimal totalAmount,
        List<OrderItemDTO> items
) {
    public record OrderItemDTO(Long itemId, String productId, int quantity, BigDecimal price) {
    }

    public static OrderSummary from(Order order) {
        List<OrderItemDTO> items = order.getItems().stream()
                .map(i -> new OrderItemDTO(i.getId(), i.getProductId(), i.getQuantity(), i.getPrice()))
                .toList();
        return new OrderSummary(order.getId(), order.getOrderState().name(), order.getTotalAmount(), items);
    }
}
