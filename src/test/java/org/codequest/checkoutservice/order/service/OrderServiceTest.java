package org.codequest.checkoutservice.order.service;

import org.codequest.checkoutservice.order.domain.Order;
import org.codequest.checkoutservice.order.exception.OrderException;
import org.codequest.checkoutservice.order.repository.OrderRepository;
import org.codequest.checkoutservice.shared.exception.ResourceNotFoundException;
import org.codequest.checkoutservice.shared.facade.payment.PaymentFacade;
import org.codequest.checkoutservice.shared.model.cart.CartCheckoutData;
import org.codequest.checkoutservice.shared.model.order.OrderState;
import org.codequest.checkoutservice.shared.model.payment.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    PaymentFacade paymentFacade;

    @InjectMocks
    OrderService orderService;

    private CartCheckoutData cartData() {
        return new CartCheckoutData(1L, List.of(
                new CartCheckoutData.CartItemData("P1", 2, new BigDecimal("5.00"))
        ), BigDecimal.TEN);
    }

    // --- createOrder ---

    @Test
    void createOrder_whenNoExistingOrder_createsAndSavesNew() {
        when(orderRepository.findByCartId(1L)).thenReturn(Optional.empty());
        Order saved = new Order(1L, BigDecimal.TEN);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        Order result = orderService.createOrder(cartData());

        assertThat(result).isSameAs(saved);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_whenOrderAlreadyExists_returnsExistingWithoutSaving() {
        Order existing = new Order(1L, BigDecimal.TEN);
        when(orderRepository.findByCartId(1L)).thenReturn(Optional.of(existing));

        Order result = orderService.createOrder(cartData());

        assertThat(result).isSameAs(existing);
        verify(orderRepository, never()).save(any());
    }

    // --- cancelOrder ---

    @Test
    void cancelOrder_whenFound_transitionsToCancel() {
        Order order = new Order(1L, BigDecimal.TEN);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.cancelOrder(1L);

        assertThat(result.getOrderState()).isEqualTo(OrderState.CANCELLED);
    }

    @Test
    void cancelOrder_whenNotFound_throwsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getOrder ---

    @Test
    void getOrder_whenFound_returnsOrder() {
        Order order = new Order(1L, BigDecimal.TEN);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Order result = orderService.getOrder(1L);

        assertThat(result).isSameAs(order);
    }

    @Test
    void getOrder_whenNotFound_throwsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- payForOrder ---

    @Test
    void payForOrder_whenOrderIsPayable_startsPaymentAndTransitionsToPending() {
        Order order = new Order(1L, BigDecimal.TEN);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        PaymentResponse response = new PaymentResponse(UUID.randomUUID(), 1L, "ext-1", BigDecimal.TEN, "PENDING");
        when(paymentFacade.startPayment(any(), any())).thenReturn(response);
        when(orderRepository.save(order)).thenReturn(order);

        PaymentResponse result = orderService.payForOrder(1L);

        assertThat(result).isSameAs(response);
        assertThat(order.getOrderState()).isEqualTo(OrderState.PENDING_PAYMENT);
    }

    @Test
    void payForOrder_whenOrderIsNotPayable_throwsOrderException() {
        Order order = new Order(1L, BigDecimal.TEN);
        order.transitionTo(OrderState.PENDING_PAYMENT); // not payable
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.payForOrder(1L))
                .isInstanceOf(OrderException.class);

        verifyNoInteractions(paymentFacade);
    }

    @Test
    void payForOrder_afterPaymentFailed_allowsRetry() {
        Order order = new Order(1L, BigDecimal.TEN);
        order.transitionTo(OrderState.PENDING_PAYMENT);
        order.transitionTo(OrderState.PAYMENT_FAILED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        PaymentResponse response = new PaymentResponse(UUID.randomUUID(), 1L, "ext-2", BigDecimal.TEN, "PENDING");
        when(paymentFacade.startPayment(any(), any())).thenReturn(response);
        when(orderRepository.save(order)).thenReturn(order);

        orderService.payForOrder(1L);

        assertThat(order.getOrderState()).isEqualTo(OrderState.PENDING_PAYMENT);
    }

    // --- transitOrder ---

    @Test
    void transitOrder_whenFound_appliesTransition() {
        Order order = new Order(1L, BigDecimal.TEN);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.transitOrder(1L, OrderState.PENDING_PAYMENT);

        assertThat(order.getOrderState()).isEqualTo(OrderState.PENDING_PAYMENT);
    }

    @Test
    void transitOrder_whenNotFound_throwsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.transitOrder(99L, OrderState.PAID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
