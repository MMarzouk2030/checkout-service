package org.codequest.checkoutservice.payment.domain;

import jakarta.persistence.*;
import org.codequest.checkoutservice.payment.exception.PaymentErrorCode;
import org.codequest.checkoutservice.payment.exception.PaymentException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, unique = true)
    private String externalPaymentId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    protected Payment() {
    }

    public Payment(Long orderId, String externalPaymentId, BigDecimal amount) {
        this.orderId = orderId;
        this.externalPaymentId = externalPaymentId;
        this.amount = amount;
    }

    public UUID getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getExternalPaymentId() {
        return externalPaymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isTerminal() {
        return status == PaymentStatus.CONFIRMED || status == PaymentStatus.FAILED;
    }

    public void confirm() {
        if (status != PaymentStatus.PENDING) {
            throw new PaymentException(PaymentErrorCode.CONFIRM_PAYMENT_INVALID_STATUS, status);
        }
        status = PaymentStatus.CONFIRMED;
    }

    public void fail() {
        if (status != PaymentStatus.PENDING) {
            throw new PaymentException(PaymentErrorCode.FAIL_PAYMENT_INVALID_STATUS, status);
        }
        status = PaymentStatus.FAILED;
    }
}
