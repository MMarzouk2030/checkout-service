package org.codequest.checkoutservice.cart.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Cart", description = "Create carts, add items, and checkout to create an Order")
@RestController
@RequestMapping("/carts")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "Create a new empty cart")
    @ApiResponse(responseCode = "201", description = "Cart created")
    @PostMapping
    public ResponseEntity<CartResponse> createCart() {
        log.info("POST /carts");
        Cart cart = cartService.createCart();
        return ResponseEntity.status(HttpStatus.CREATED).body(CartResponse.from(cart));
    }

    @Operation(summary = "Get a cart by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart found"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/{cartId}")
    public ResponseEntity<CartResponse> getCart(
            @Parameter(description = "Cart ID") @PathVariable Long cartId) {
        log.info("GET /carts/{}", cartId);
        Cart cart = cartService.getCart(cartId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @Operation(summary = "Add an item to a cart",
               description = "Price is supplied by the caller. Quantity must be ≥ 1. Price must be > 0.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item added"),
            @ApiResponse(responseCode = "400", description = "Cart already checked out, invalid quantity/price"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @PostMapping("/{cartId}/items")
    public ResponseEntity<CartResponse> addItem(
            @Parameter(description = "Cart ID") @PathVariable Long cartId,
            @RequestBody AddItemRequest request) {
        log.info("POST /carts/{}/items [productId={}, quantity={}]",
                cartId, request.productId(), request.quantity());
        Cart cart = cartService.addItem(cartId, request.productId(), request.quantity(), request.price());
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    @Operation(summary = "Checkout a cart",
               description = "Marks the cart CHECKED_OUT and creates an Order in CREATED state. Idempotent — "
                             + "re-checking the same cart returns the existing Order.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created"),
            @ApiResponse(responseCode = "400", description = "Cart already checked out or is empty"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @PostMapping("/{cartId}/checkout")
    public ResponseEntity<OrderSummary> checkout(
            @Parameter(description = "Cart ID") @PathVariable Long cartId) {
        log.info("POST /carts/{}/checkout", cartId);
        OrderSummary summary = cartService.checkout(cartId);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }
}
