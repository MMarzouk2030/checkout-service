package org.codequest.checkoutservice.payment.exception;

public enum PaymentErrorCode {
    CONFIRM_PAYMENT_INVALID_STATUS("payment.error.confirm_payment.invalid_state_transition"),
    FAIL_PAYMENT_INVALID_STATUS("payment.error.fail_payment.invalid_state_transition"),
    PAYMENT_ALREADY_COMPLETED("payment.error.payment.already_completed"),
    PAYMENT_ALREADY_EXISTS("payment.error.payment.already.existing.and.pending"),
    INVALID_WEBHOOK_STATUS("payment.error.invalid_webhook_status");

    private final String messageKey;

    PaymentErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
