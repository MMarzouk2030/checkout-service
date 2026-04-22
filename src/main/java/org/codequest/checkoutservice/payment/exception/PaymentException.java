package org.codequest.checkoutservice.payment.exception;

public class PaymentException extends RuntimeException {
    private final PaymentErrorCode errorCode;
    private final Object[] args;

    public PaymentException(PaymentErrorCode errorCode, Object... args) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.args = args;
    }

    public PaymentErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
