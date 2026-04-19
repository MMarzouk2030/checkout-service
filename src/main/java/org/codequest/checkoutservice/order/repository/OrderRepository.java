package org.codequest.checkoutservice.order.repository;

import org.codequest.checkoutservice.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByCartId(Long cartId);
}
