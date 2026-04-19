package org.codequest.checkoutservice.order.domain;

import org.codequest.checkoutservice.order.exception.OrderErrorCode;
import org.codequest.checkoutservice.order.exception.OrderException;

import java.util.Map;
import java.util.Set;

import static org.codequest.checkoutservice.order.domain.OrderState.*;

/**
 * Order state machine.
 * <p>
 * Valid transitions:
 * CREATED         → PENDING_PAYMENT, CANCELLED
 * PENDING_PAYMENT → PAID, PAYMENT_FAILED, CANCELLED
 * PAYMENT_FAILED  → PENDING_PAYMENT, CANCELLED
 * PAID            → (terminal)
 * CANCELLED       → (terminal)
 */
public class OrderStateMachine {

    private static final Map<OrderState, Set<OrderState>> VALID_TRANSITIONS = Map.of(
            CREATED, Set.of(PENDING_PAYMENT, CANCELLED),
            PENDING_PAYMENT, Set.of(PAID, PAYMENT_FAILED, CANCELLED),
            PAYMENT_FAILED, Set.of(PENDING_PAYMENT, CANCELLED),
            PAID, Set.of(),
            CANCELLED, Set.of()
    );

    public static void validateTransition(OrderState from, OrderState to) {
        Set<OrderState> validTransitions = VALID_TRANSITIONS.getOrDefault(from, Set.of());

        if (!validTransitions.contains(to)) {
            throw new OrderException(OrderErrorCode.INVALID_STATE_TRANSITION);
        }
    }
}
