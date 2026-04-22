package org.codequest.checkoutservice.shared.facade.order;

import org.codequest.checkoutservice.shared.model.order.OrderState;
import org.codequest.checkoutservice.shared.model.cart.CartCheckoutData;
import org.codequest.checkoutservice.shared.model.order.OrderSummary;

public interface OrderFacade {

    OrderSummary createOrder(CartCheckoutData data);

    void transitOrder(Long orderId, OrderState orderState);

}
