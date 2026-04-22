package org.codequest.checkoutservice.order.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.codequest.checkoutservice.order.domain.Order;
import org.codequest.checkoutservice.order.service.OrderService;
import org.codequest.checkoutservice.shared.model.order.OrderSummary;
import org.codequest.checkoutservice.shared.model.payment.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Order", description = "Manage orders: fetch status, start payment, or cancel")
@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Get an order by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderSummary> getOrder(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        log.info("GET /orders/{}", orderId);
        Order order = orderService.getOrder(orderId);
        return ResponseEntity.ok(OrderSummary.from(order));
    }

    @Operation(summary = "Start payment for an order",
               description = "Contacts the payment provider and transitions the order to PENDING_PAYMENT. "
                             + "Idempotent if already PENDING_PAYMENT. Can be retried after PAYMENT_FAILED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment started — returns externalPaymentId"),
            @ApiResponse(responseCode = "400", description = "Order not in a payable state, or payment already pending"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PostMapping("/{orderId}/payment/start")
    public ResponseEntity<PaymentResponse> startPayment(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        log.info("POST /orders/{}/payment/start", orderId);
        PaymentResponse paymentResponse = orderService.payForOrder(orderId);
        return ResponseEntity.ok(paymentResponse);
    }

    @Operation(summary = "Cancel an order",
               description = "Cancels the order if it is in CREATED or PAYMENT_FAILED state. "
                             + "PENDING_PAYMENT, PAID, and CANCELLED orders cannot be cancelled.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled"),
            @ApiResponse(responseCode = "400", description = "Order state does not allow cancellation"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderSummary> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        log.info("PUT /orders/{}/cancel", orderId);
        Order order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(OrderSummary.from(order));
    }
}
