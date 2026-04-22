package org.codequest.checkoutservice.cart.service;

import org.codequest.checkoutservice.cart.domain.Cart;
import org.codequest.checkoutservice.cart.repository.CartRepository;
import org.codequest.checkoutservice.shared.exception.ResourceNotFoundException;
import org.codequest.checkoutservice.shared.facade.order.OrderFacade;
import org.codequest.checkoutservice.shared.model.order.OrderSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    CartRepository cartRepository;
    @Mock
    OrderFacade orderFacade;

    @InjectMocks
    CartService cartService;

    @Test
    void createCart_savesAndReturnsNewCart() {
        Cart saved = new Cart();
        when(cartRepository.save(any(Cart.class))).thenReturn(saved);

        Cart result = cartService.createCart();

        assertThat(result).isSameAs(saved);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItem_whenCartFound_addsItemAndSaves() {
        Cart cart = new Cart();
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);

        Cart result = cartService.addItem(1L, "P1", 2, new BigDecimal("9.99"));

        assertThat(result.getItems()).hasSize(1);
        verify(cartRepository).save(cart);
    }

    @Test
    void addItem_whenCartNotFound_throwsResourceNotFoundException() {
        when(cartRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(99L, "P1", 1, BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCart_whenFound_returnsCart() {
        Cart cart = new Cart();
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));

        Cart result = cartService.getCart(1L);

        assertThat(result).isSameAs(cart);
    }

    @Test
    void getCart_whenNotFound_throwsResourceNotFoundException() {
        when(cartRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCart(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void checkout_locksCartAndDelegatesOrderCreation() {
        Cart cart = new Cart();
        cart.addItem("P1", 1, BigDecimal.TEN);
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(cart)).thenReturn(cart);

        OrderSummary expected = new OrderSummary(1L, "CREATED", BigDecimal.TEN, List.of());
        when(orderFacade.createOrder(any())).thenReturn(expected);

        OrderSummary result = cartService.checkout(1L);

        assertThat(result).isSameAs(expected);
        verify(orderFacade).createOrder(any());
    }

    @Test
    void checkout_whenCartNotFound_throwsResourceNotFoundException() {
        when(cartRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.checkout(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(orderFacade);
    }
}
