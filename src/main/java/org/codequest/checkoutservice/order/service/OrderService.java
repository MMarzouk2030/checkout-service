package org.codequest.checkoutservice.order.service;

import org.codequest.checkoutservice.order.domain.Order;
import org.codequest.checkoutservice.order.domain.OrderState;
import org.codequest.checkoutservice.order.repository.OrderRepository;
import org.codequest.checkoutservice.shared.exception.ErrorCode;
import org.codequest.checkoutservice.shared.exception.ResourceNotFoundException;
import org.codequest.checkoutservice.shared.model.CartCheckoutData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order createOrder(CartCheckoutData data) {
        // Idempotent: return existing order if already created for this cart
        return orderRepository.findByCartId(data.cartId())
                .orElseGet(() -> buildAndSave(data));
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = findOrder(orderId);
        order.transitionTo(OrderState.CANCELLED);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return findOrder(orderId);
    }

    /**
     * Create the order with items copied from the cart
     *
     * @param data cart data for all items listed in cart before already
     * @return the created order
     */
    private Order buildAndSave(CartCheckoutData data) {
        Order order = new Order(data.cartId(), data.totalAmount());
        data.items().forEach(item ->
                order.addItem(item.productId(), item.quantity(), item.price())
        );
        return orderRepository.save(order);
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND));
    }
}
