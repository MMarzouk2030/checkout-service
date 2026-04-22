package org.codequest.checkoutservice.order.exception;

public class OrderException extends RuntimeException {

    private final OrderErrorCode errorCode;
    private final Object[] args;

    public OrderException(OrderErrorCode errorCode, Object... args) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.args = args;
    }

    public OrderErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
