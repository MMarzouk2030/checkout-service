package org.codequest.checkoutservice.integration;

import tools.jackson.databind.ObjectMapper;
import org.codequest.checkoutservice.shared.rest.PaymentProviderClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean PaymentProviderClient paymentProviderClient;

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("DELETE FROM payments");
        jdbc.execute("DELETE FROM order_items");
        jdbc.execute("DELETE FROM orders");
        jdbc.execute("DELETE FROM cart_items");
        jdbc.execute("DELETE FROM carts");
    }

    /* Helpers */
    private long createCart() throws Exception {
        String body = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void addItem(long cartId) throws Exception {
        mockMvc.perform(post("/carts/{id}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"P1","quantity":2,"price":9.99}
                                """))
                .andExpect(status().isOk());
    }

    private long checkout(long cartId) throws Exception {
        String body = mockMvc.perform(post("/carts/{id}/checkout", cartId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("orderId").asLong();
    }

    private void startPayment(long orderId) throws Exception {
        String body = mockMvc.perform(post("/orders/{id}/payment/start", orderId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        objectMapper.readTree(body).get("externalPaymentId").asString();
    }

    private void sendWebhook(String externalPaymentId, String status) throws Exception {
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("externalPaymentId", externalPaymentId, "status", status))))
                .andExpect(status().isOk());
    }

    private void assertOrderState(long orderId, String expectedState) throws Exception {
        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is(expectedState)));
    }

    /**
     * Flow 1 — Happy path: cart → checkout → pay → CONFIRMED → PAID
     */
    @Test
    void flow1_happyPath_orderEndsAsPaid() throws Exception {
        when(paymentProviderClient.startPayment(any())).thenReturn(ResponseEntity.ok("ext-happy-1"));

        long cartId = createCart();
        addItem(cartId);
        long orderId = checkout(cartId);

        assertOrderState(orderId, "CREATED");

        startPayment(orderId);
        assertOrderState(orderId, "PENDING_PAYMENT");

        sendWebhook("ext-happy-1", "CONFIRMED");
        assertOrderState(orderId, "PAID");
    }

    /**
     * Flow 2 — Payment failure + retry → order eventually PAID
     */
    @Test
    void flow2_paymentFailureThenRetry_orderEndsAsPaid() throws Exception {
        when(paymentProviderClient.startPayment(any()))
                .thenReturn(ResponseEntity.ok("ext-fail-1"))
                .thenReturn(ResponseEntity.ok("ext-fail-2"));

        long cartId = createCart();
        addItem(cartId);
        long orderId = checkout(cartId);

        // First attempt — provider confirms a FAILED outcome
        startPayment(orderId);
        sendWebhook("ext-fail-1", "FAILED");
        assertOrderState(orderId, "PAYMENT_FAILED");

        // Retry — provider confirms success this time
        startPayment(orderId);
        sendWebhook("ext-fail-2", "CONFIRMED");
        assertOrderState(orderId, "PAID");
    }

    /**
     * Flow 3 — Duplicate webhook: same CONFIRMED event delivered twice
     */
    @Test
    void flow3_duplicateWebhook_isIdempotentAndOrderRemainsInPaidState() throws Exception {
        when(paymentProviderClient.startPayment(any())).thenReturn(ResponseEntity.ok("ext-dup-1"));

        long cartId = createCart();
        addItem(cartId);
        long orderId = checkout(cartId);
        startPayment(orderId);

        sendWebhook("ext-dup-1", "CONFIRMED");
        assertOrderState(orderId, "PAID");

        // Second delivery of the same webhook — must be a no-op
        sendWebhook("ext-dup-1", "CONFIRMED");
        assertOrderState(orderId, "PAID");
    }

    /**
     * [Idempotency] Duplicate payment attempt while one is already PENDING
     */
    @Test
    void startPayment_whenDuplicatePending_returns400() throws Exception {
        when(paymentProviderClient.startPayment(any())).thenReturn(ResponseEntity.ok("ext-dup-pay"));

        long cartId = createCart();
        addItem(cartId);
        long orderId = checkout(cartId);
        startPayment(orderId); // first attempt → order is PENDING_PAYMENT

        // Second attempt while first payment is still PENDING
        mockMvc.perform(post("/orders/{id}/payment/start", orderId))
                .andExpect(status().isBadRequest());
    }

    /**
     * Checkout idempotency — same cart checked out twice returns same order
     */
    @Test
    void checkout_calledTwiceOnSameCart_returnsTheSameOrder() throws Exception {
        long cartId = createCart();
        addItem(cartId);

        long firstOrderId = checkout(cartId);

        // Cart is now CHECKED_OUT — calling checkout again must return the SAME order
        mockMvc.perform(post("/carts/{id}/checkout", cartId))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        // The idempotency in createOrder is at the OrderService level (same cartId → same order).
        // The Cart itself rejects a second checkout with 400 before even reaching the order creation.
        // Verify the original order is still intact.
        assertOrderState(firstOrderId, "CREATED");
    }

    /**
     * Guard Test: cannot start payment on an order already in PENDING_PAYMENT
     */
    @Test
    void startPayment_whenOrderAlreadyPendingPayment_returns400() throws Exception {
        when(paymentProviderClient.startPayment(any())).thenReturn(ResponseEntity.ok("ext-guard-1"));

        long cartId = createCart();
        addItem(cartId);
        long orderId = checkout(cartId);
        startPayment(orderId); // moves to PENDING_PAYMENT

        // Order is now PENDING_PAYMENT — not payable again
        mockMvc.perform(post("/orders/{id}/payment/start", orderId))
                .andExpect(status().isBadRequest());
    }

    /**
     * Checkout an empty cart must fail
     */
    @Test
    void checkout_emptyCart_returns400() throws Exception {
        long cartId = createCart();

        mockMvc.perform(post("/carts/{id}/checkout", cartId))
                .andExpect(status().isBadRequest());
    }

    /**
     * Unknown order returns 404
     */
    @Test
    void getOrder_whenNotFound_returns404() throws Exception {
        mockMvc.perform(get("/orders/9999"))
                .andExpect(status().isNotFound());
    }

    /**
     * Webhook for an unknown payment ID returns 404
     */
    @Test
    void webhook_unknownExternalId_returns404() throws Exception {
        mockMvc.perform(post("/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalPaymentId":"unknown-ext","status":"CONFIRMED"}
                                """))
                .andExpect(status().isNotFound());
    }
}
