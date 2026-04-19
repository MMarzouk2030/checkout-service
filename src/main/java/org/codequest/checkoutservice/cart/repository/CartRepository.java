package org.codequest.checkoutservice.cart.repository;

import org.codequest.checkoutservice.cart.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
}
