package org.codequest.checkoutservice.cart.exception;

public class CartException extends RuntimeException {

    private final CartErrorCode errorCode;
    private final Object[] args;

    public CartException(CartErrorCode errorCode, Object... args) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.args = args;
    }

    public CartErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
