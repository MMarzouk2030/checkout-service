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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final PaymentFacade paymentFacade;

    public OrderService(OrderRepository orderRepository, PaymentFacade paymentFacade) {
        this.orderRepository = orderRepository;
        this.paymentFacade = paymentFacade;
    }

    @Transactional
    public Order createOrder(CartCheckoutData data) {
        return orderRepository.findByCartId(data.cartId())
                .map(existing -> {
                    log.info("Order already exists for cart, returning existing [cartId={}, orderId={}]",
                            data.cartId(), existing.getId());
                    return existing;
                })
                .orElseGet(() -> {
                    Order order = buildAndSave(data);
                    log.info("Order created [orderId={}, cartId={}, total={}]",
                            order.getId(), data.cartId(), data.totalAmount());
                    return order;
                });
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = findOrder(orderId);
        order.transitionTo(OrderState.CANCELLED);
        Order saved = orderRepository.save(order);
        log.info("Order cancelled [orderId={}]", orderId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return findOrder(orderId);
    }

    @Transactional
    public PaymentResponse payForOrder(Long orderId) {
        Order order = validateOrderPayableState(orderId);
        log.info("Starting payment for order [orderId={}, total={}]", orderId, order.getTotalAmount());

        PaymentResponse paymentResponse = paymentFacade.startPayment(order.getId(), order.getTotalAmount());

        order.transitionTo(OrderState.PENDING_PAYMENT);
        orderRepository.save(order);

        log.info("Payment initiated [orderId={}, externalPaymentId={}]",
                orderId, paymentResponse.externalPaymentId());
        return paymentResponse;
    }

    @Transactional
    public void transitOrder(Long orderId, OrderState orderState) {
        Order order = findOrder(orderId);
        OrderState previousState = order.getOrderState();
        order.transitionTo(orderState);
        orderRepository.save(order);
        log.info("Order state transitioned [orderId={}, from={}, to={}]",
                orderId, previousState, orderState);
    }

    private Order validateOrderPayableState(Long orderId) {
        Order order = findOrder(orderId);
        if (!order.isInPayableState()) {
            log.warn("Order is not in a payable state [orderId={}, currentState={}]",
                    orderId, order.getOrderState());
            throw new OrderException(OrderErrorCode.INVALID_ORDER_PAYABLE_STATE, order.getOrderState());
        }
        return order;
    }

    private Order buildAndSave(CartCheckoutData data) {
        Order order = new Order(data.cartId(), data.totalAmount());
        data.items().forEach(item ->
                order.addItem(item.productId(), item.quantity(), item.price())
        );
        return orderRepository.save(order);
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found [orderId={}]", orderId);
                    return new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND);
                });
    }
}
