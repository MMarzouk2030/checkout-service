package org.codequest.checkoutservice.shared.facade.order;

import org.codequest.checkoutservice.order.domain.Order;
import org.codequest.checkoutservice.shared.model.order.OrderState;
import org.codequest.checkoutservice.order.service.OrderService;
import org.codequest.checkoutservice.shared.model.cart.CartCheckoutData;
import org.codequest.checkoutservice.shared.model.order.OrderSummary;
import org.springframework.stereotype.Component;

@Component
public class OrderFacadeImpl implements OrderFacade {

    private final OrderService orderService;

    public OrderFacadeImpl(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public OrderSummary createOrder(CartCheckoutData data) {
        Order order = orderService.createOrder(data);
        return OrderSummary.from(order);
    }

    @Override
    public void transitOrder(Long orderId, OrderState orderState) {
        orderService.transitOrder(orderId, orderState);
    }

}
