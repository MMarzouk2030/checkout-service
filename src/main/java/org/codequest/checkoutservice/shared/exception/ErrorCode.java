package org.codequest.checkoutservice.shared.exception;

public enum ErrorCode {

    CART_NOT_FOUND("resource.not.found.error.cart"),
    ORDER_NOT_FOUND("resource.not.found.error.order"),
    PAYMENT_NOT_FOUND("resource.not.found.error.payment"),
    PAYMENT_INTENT_NOT_FOUND("resource.not.found.error.payment.intent");

    private final String messageKey;

    ErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
