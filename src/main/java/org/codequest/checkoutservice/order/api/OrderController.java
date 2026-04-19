package org.codequest.checkoutservice.order.api;

import org.codequest.checkoutservice.order.domain.Order;
import org.codequest.checkoutservice.order.service.OrderService;
import org.codequest.checkoutservice.shared.model.OrderSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
//    private final PaymentService paymentService;

//    public OrderController(OrderService orderService, PaymentService paymentService) {
//        this.orderService = orderService;
//        this.paymentService = paymentService;
//    }

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderSummary> getOrder(@PathVariable Long orderId) {
        Order order = orderService.getOrder(orderId);
        return ResponseEntity.ok(OrderSummary.from(order));
    }

//    @PostMapping("/{orderId}/payment/start")
//    public ResponseEntity<PaymentResponse> startPayment(@PathVariable UUID orderId) {
//        Payment payment = paymentService.startPayment(orderId);
//        return ResponseEntity.ok(PaymentResponse.from(payment));
//    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderSummary> cancelOrder(@PathVariable Long orderId) {
        Order order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(OrderSummary.from(order));
    }
}
