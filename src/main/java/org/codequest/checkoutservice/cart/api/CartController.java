package org.codequest.checkoutservice.cart.api;

import org.codequest.checkoutservice.cart.domain.Cart;
import org.codequest.checkoutservice.cart.dto.AddItemRequest;
import org.codequest.checkoutservice.cart.dto.CartResponse;
import org.codequest.checkoutservice.cart.service.CartService;
import org.codequest.checkoutservice.shared.model.order.OrderSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carts")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<CartResponse> createCart() {
        log.info("POST /carts");
        Cart cart = cartService.createCart();
        return ResponseEntity.status(HttpStatus.CREATED).body(CartResponse.from(cart));
    }

    @GetMapping("/{cartId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long cartId) {
        log.info("GET /carts/{}", cartId);
        Cart cart = cartService.getCart(cartId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @PostMapping("/{cartId}/items")
    public ResponseEntity<CartResponse> addItem(@PathVariable Long cartId,
                                                @RequestBody AddItemRequest request) {
        log.info("POST /carts/{}/items [productId={}, quantity={}]",
                cartId, request.productId(), request.quantity());
        Cart cart = cartService.addItem(cartId, request.productId(), request.quantity(), request.price());
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @PostMapping("/{cartId}/checkout")
    public ResponseEntity<OrderSummary> checkout(@PathVariable Long cartId) {
        log.info("POST /carts/{}/checkout", cartId);
        OrderSummary summary = cartService.checkout(cartId);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }
}
