package org.codequest.checkoutservice.cart.exception;

public class CartException extends RuntimeException {
    private final CartErrorCode errorCode;

    public CartException(CartErrorCode errorCode) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
    }

    public CartErrorCode getErrorCode() {
        return errorCode;
    }
}
