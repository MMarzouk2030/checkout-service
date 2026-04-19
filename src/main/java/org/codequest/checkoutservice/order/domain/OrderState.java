package org.codequest.checkoutservice.order.domain;

public enum OrderState {

    CREATED,
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELLED

}
