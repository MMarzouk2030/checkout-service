package org.codequest.checkoutservice.order.domain;

import jakarta.persistence.*;
import org.codequest.checkoutservice.shared.model.order.OrderState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long cartId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderState orderState = OrderState.CREATED;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

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

    protected Order() {
    }

    public Order(Long cartId, BigDecimal totalAmount) {
        this.cartId = cartId;
        this.totalAmount = totalAmount;
    }

    public Long getId() {
        return id;
    }

    public Long getCartId() {
        return cartId;
    }

    public OrderState getOrderState() {
        return orderState;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void transitionTo(OrderState newState) {
        OrderStateMachine.validateTransition(orderState, newState);
        this.orderState = newState;
    }

    public void addItem(String productId, int quantity, BigDecimal price) {
        items.add(new OrderItem(this, productId, quantity, price));
    }

    public boolean isInPayableState() {
        return this.orderState == OrderState.CREATED || this.orderState == OrderState.PAYMENT_FAILED;
    }
}
