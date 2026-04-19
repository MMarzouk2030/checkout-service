package org.codequest.checkoutservice.cart.exception;

public enum CartErrorCode {
    // addItem errors
    CART_NOT_ACTIVE("cart.error.not_active"),
    INVALID_QUANTITY("cart.error.invalid_quantity"),
    INVALID_PRICE("cart.error.invalid_price"),

    // checkout errors
    CART_ALREADY_CHECKED_OUT("cart.error.already_checked_out"),
    CART_EMPTY("cart.error.empty");

    private final String messageKey;

    CartErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
