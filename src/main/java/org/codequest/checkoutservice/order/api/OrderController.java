package org.codequest.checkoutservice.order.api;

import org.codequest.checkoutservice.order.domain.Order;
import org.codequest.checkoutservice.order.service.OrderService;
import org.codequest.checkoutservice.shared.model.order.OrderSummary;
import org.codequest.checkoutservice.shared.model.payment.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderSummary> getOrder(@PathVariable Long orderId) {
        log.info("GET /orders/{}", orderId);
        Order order = orderService.getOrder(orderId);
        return ResponseEntity.ok(OrderSummary.from(order));
    }

    @PostMapping("/{orderId}/payment/start")
    public ResponseEntity<PaymentResponse> startPayment(@PathVariable Long orderId) {
        log.info("POST /orders/{}/payment/start", orderId);
        PaymentResponse paymentResponse = orderService.payForOrder(orderId);
        return ResponseEntity.ok(paymentResponse);
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderSummary> cancelOrder(@PathVariable Long orderId) {
        log.info("PUT /orders/{}/cancel", orderId);
        Order order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(OrderSummary.from(order));
    }
}
