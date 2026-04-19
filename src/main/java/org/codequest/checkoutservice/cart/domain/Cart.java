package org.codequest.checkoutservice.cart.domain;

import jakarta.persistence.*;
import org.codequest.checkoutservice.cart.exception.CartErrorCode;
import org.codequest.checkoutservice.cart.exception.CartException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status = CartStatus.ACTIVE;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<CartItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CartStatus getStatus() {
        return status;
    }

    public List<CartItem> getItems() {
        return List.copyOf(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void addItem(String productId, int quantity, BigDecimal price) {
        if (status != CartStatus.ACTIVE) {
            throw new CartException(CartErrorCode.CART_NOT_ACTIVE);
        }
        if (quantity <= 0) {
            throw new CartException(CartErrorCode.INVALID_QUANTITY);
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CartException(CartErrorCode.INVALID_PRICE);
        }
        items.add(new CartItem(this, productId, quantity, price));
    }

    public void checkout() {
        if (status != CartStatus.ACTIVE) {
            throw new CartException(CartErrorCode.CART_ALREADY_CHECKED_OUT);
        }
        if (items.isEmpty()) {
            throw new CartException(CartErrorCode.CART_EMPTY);
        }
        status = CartStatus.CHECKED_OUT;
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
