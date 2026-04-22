package org.codequest.checkoutservice.cart.service;

import org.codequest.checkoutservice.cart.domain.Cart;
import org.codequest.checkoutservice.cart.repository.CartRepository;
import org.codequest.checkoutservice.shared.model.cart.CartCheckoutData;
import org.codequest.checkoutservice.shared.facade.order.OrderFacade;
import org.codequest.checkoutservice.shared.model.order.OrderSummary;
import org.codequest.checkoutservice.shared.exception.ErrorCode;
import org.codequest.checkoutservice.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final OrderFacade orderFacade;

    public CartService(CartRepository cartRepository, OrderFacade orderFacade) {
        this.cartRepository = cartRepository;
        this.orderFacade = orderFacade;
    }

    @Transactional
    public Cart createCart() {
        Cart cart = cartRepository.save(new Cart());
        log.info("Cart created [cartId={}]", cart.getId());
        return cart;
    }

    @Transactional
    public Cart addItem(Long cartId, String productId, int quantity, BigDecimal price) {
        Cart cart = findCart(cartId);
        cart.addItem(productId, quantity, price);
        Cart saved = cartRepository.save(cart);
        log.info("Item added to cart [cartId={}, productId={}, quantity={}, price={}]",
                cartId, productId, quantity, price);
        return saved;
    }

    @Transactional(readOnly = true)
    public Cart getCart(Long cartId) {
        return findCart(cartId);
    }

    @Transactional
    public OrderSummary checkout(Long cartId) {
        log.info("Checkout initiated [cartId={}]", cartId);
        Cart cart = findCart(cartId);
        cart.checkout();
        cartRepository.save(cart);
        OrderSummary summary = orderFacade.createOrder(toCartCheckoutData(cart));
        log.info("Checkout completed [cartId={}, orderId={}]", cartId, summary.orderId());
        return summary;
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
                .orElseThrow(() -> {
                    log.warn("Cart not found [cartId={}]", cartId);
                    return new ResourceNotFoundException(ErrorCode.CART_NOT_FOUND);
                });
    }
}
