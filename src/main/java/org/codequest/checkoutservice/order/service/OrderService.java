package org.codequest.checkoutservice.order.service;

import org.codequest.checkoutservice.order.domain.Order;
import org.codequest.checkoutservice.shared.model.order.OrderState;
import org.codequest.checkoutservice.order.exception.OrderErrorCode;
import org.codequest.checkoutservice.order.exception.OrderException;
import org.codequest.checkoutservice.order.repository.OrderRepository;
import org.codequest.checkoutservice.shared.exception.ErrorCode;
import org.codequest.checkoutservice.shared.exception.ResourceNotFoundException;
import org.codequest.checkoutservice.shared.facade.payment.PaymentFacade;
import org.codequest.checkoutservice.shared.model.cart.CartCheckoutData;
import org.codequest.checkoutservice.shared.model.payment.PaymentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentFacade paymentFacade;

    public OrderService(OrderRepository orderRepository, PaymentFacade paymentFacade) {
        this.orderRepository = orderRepository;
        this.paymentFacade = paymentFacade;
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

    public PaymentResponse payForOrder(Long orderId) {
        Order order = validateOrderPayableState(orderId);

        PaymentResponse paymentResponse = paymentFacade.startPayment(order.getId(), order.getTotalAmount());

        order.transitionTo(OrderState.PENDING_PAYMENT);
        orderRepository.save(order);

        return paymentResponse;
    }

    @Transactional
    public void transitOrder(Long orderId, OrderState orderState) {
        Order order = findOrder(orderId);

        order.transitionTo(orderState);

        orderRepository.save(order);
    }

    private Order validateOrderPayableState(Long orderId) {
        Order order = findOrder(orderId);
        if (!order.isInPayableState()) {
            throw new OrderException(OrderErrorCode.INVALID_ORDER_PAYABLE_STATE, order.getOrderState());
        }

        return order;
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
