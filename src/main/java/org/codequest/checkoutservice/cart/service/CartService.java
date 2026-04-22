package org.codequest.checkoutservice.cart.service;

import org.codequest.checkoutservice.cart.domain.Cart;
import org.codequest.checkoutservice.cart.repository.CartRepository;
import org.codequest.checkoutservice.shared.model.cart.CartCheckoutData;
import org.codequest.checkoutservice.shared.facade.order.OrderFacade;
import org.codequest.checkoutservice.shared.model.order.OrderSummary;
import org.codequest.checkoutservice.shared.exception.ErrorCode;
import org.codequest.checkoutservice.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final OrderFacade orderFacade;

    public CartService(CartRepository cartRepository, OrderFacade orderFacade) {
        this.cartRepository = cartRepository;
        this.orderFacade = orderFacade;
    }

    @Transactional
    public Cart createCart() {
        return cartRepository.save(new Cart());
    }

    @Transactional
    public Cart addItem(Long cartId, String productId, int quantity, BigDecimal price) {
        Cart cart = findCart(cartId);
        cart.addItem(productId, quantity, price);
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public Cart getCart(Long cartId) {
        return findCart(cartId);
    }

    @Transactional
    public OrderSummary checkout(Long cartId) {
        Cart cart = findCart(cartId);
        // Lock the cart
        cart.checkout();
        cartRepository.save(cart);

        return orderFacade.createOrder(toCartCheckoutData(cart));
    }

    private CartCheckoutData toCartCheckoutData(Cart cart) {
        List<CartCheckoutData.CartItemData> items = cart.getItems().stream()
                .map(item -> new CartCheckoutData.CartItemData(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice()
                ))
                .toList();
        return new CartCheckoutData(cart.getId(), items, cart.getTotal());
    }

    private Cart findCart(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CART_NOT_FOUND));
    }
}
