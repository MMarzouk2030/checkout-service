package org.codequest.checkoutservice.payment.repository;

import org.codequest.checkoutservice.payment.domain.Payment;
import org.codequest.checkoutservice.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByExternalPaymentId(String externalPaymentId);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
