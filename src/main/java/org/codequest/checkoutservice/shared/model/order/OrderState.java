package org.codequest.checkoutservice.shared.model.order;

public enum OrderState {

    CREATED,
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELLED

}
