package org.codequest.checkoutservice.order.exception;

public enum OrderErrorCode {

    INVALID_STATE_TRANSITION("order.error.invalid_state_transition"),
    INVALID_ORDER_PAYABLE_STATE("order.error.not.in.payable.state");

    private final String messageKey;

    OrderErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
