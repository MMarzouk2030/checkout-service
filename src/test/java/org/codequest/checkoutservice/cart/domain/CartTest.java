package org.codequest.checkoutservice.cart.domain;

import org.codequest.checkoutservice.cart.exception.CartErrorCode;
import org.codequest.checkoutservice.cart.exception.CartException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = new Cart();
    }

    @Test
    void newCart_isActive_andEmpty() {
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void addItem_whenActive_addsItemToCart() {
        cart.addItem("P1", 2, new BigDecimal("9.99"));
        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    void addItem_whenCheckedOut_throwsCartNotActive() {
        cart.addItem("P1", 1, BigDecimal.TEN);
        cart.checkout();

        assertThatThrownBy(() -> cart.addItem("P2", 1, BigDecimal.TEN))
                .isInstanceOf(CartException.class)
                .extracting(e -> ((CartException) e).getErrorCode())
                .isEqualTo(CartErrorCode.CART_NOT_ACTIVE);
    }

    @Test
    void addItem_whenQuantityIsZero_throwsInvalidQuantity() {
        assertThatThrownBy(() -> cart.addItem("P1", 0, BigDecimal.TEN))
                .isInstanceOf(CartException.class)
                .extracting(e -> ((CartException) e).getErrorCode())
                .isEqualTo(CartErrorCode.INVALID_QUANTITY);
    }

    @Test
    void addItem_whenQuantityIsNegative_throwsInvalidQuantity() {
        assertThatThrownBy(() -> cart.addItem("P1", -1, BigDecimal.TEN))
                .isInstanceOf(CartException.class)
                .extracting(e -> ((CartException) e).getErrorCode())
                .isEqualTo(CartErrorCode.INVALID_QUANTITY);
    }

    @Test
    void addItem_whenPriceIsNull_throwsInvalidPrice() {
        assertThatThrownBy(() -> cart.addItem("P1", 1, null))
                .isInstanceOf(CartException.class)
                .extracting(e -> ((CartException) e).getErrorCode())
                .isEqualTo(CartErrorCode.INVALID_PRICE);
    }

    @Test
    void addItem_whenPriceIsNegative_throwsInvalidPrice() {
        assertThatThrownBy(() -> cart.addItem("P1", 1, new BigDecimal("-1")))
                .isInstanceOf(CartException.class)
                .extracting(e -> ((CartException) e).getErrorCode())
                .isEqualTo(CartErrorCode.INVALID_PRICE);
    }

    @Test
    void addItem_whenPriceIsZero_throwsInvalidPrice() {
        assertThatThrownBy(() -> cart.addItem("P1", 1, BigDecimal.ZERO))
                .isInstanceOf(CartException.class)
                .extracting(e -> ((CartException) e).getErrorCode())
                .isEqualTo(CartErrorCode.INVALID_PRICE);
    }

    @Test
    void checkout_whenActiveWithItems_setsStatusToCheckedOut() {
        cart.addItem("P1", 1, BigDecimal.TEN);
        cart.checkout();

        assertThat(cart.getStatus()).isEqualTo(CartStatus.CHECKED_OUT);
    }

    @Test
    void checkout_whenEmpty_throwsCartEmpty() {
        assertThatThrownBy(() -> cart.checkout())
                .isInstanceOf(CartException.class)
                .extracting(e -> ((CartException) e).getErrorCode())
                .isEqualTo(CartErrorCode.CART_EMPTY);
    }

    @Test
    void checkout_whenAlreadyCheckedOut_throwsCartAlreadyCheckedOut() {
        cart.addItem("P1", 1, BigDecimal.TEN);
        cart.checkout();

        assertThatThrownBy(() -> cart.checkout())
                .isInstanceOf(CartException.class)
                .extracting(e -> ((CartException) e).getErrorCode())
                .isEqualTo(CartErrorCode.CART_ALREADY_CHECKED_OUT);
    }

    @Test
    void getTotal_returnsSumOfAllItemPrices() {
        cart.addItem("P1", 2, new BigDecimal("5.00"));  // 10.00
        cart.addItem("P2", 3, new BigDecimal("4.00"));  // 12.00

        assertThat(cart.getTotal()).isEqualByComparingTo(new BigDecimal("22.00"));
    }

    @Test
    void getItems_returnsUnmodifiableView() {
        cart.addItem("P1", 1, BigDecimal.TEN);

        assertThatThrownBy(() -> cart.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
