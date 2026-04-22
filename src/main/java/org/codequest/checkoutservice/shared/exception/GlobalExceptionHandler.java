package org.codequest.checkoutservice.shared.exception;

import org.codequest.checkoutservice.cart.exception.CartException;
import org.codequest.checkoutservice.order.exception.OrderException;
import org.codequest.checkoutservice.payment.exception.PaymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(CartException.class)
    public ResponseEntity<ErrorResponse> handleCartException(CartException exc) {
        log.warn("Cart business rule violation [code={}]", exc.getErrorCode());
        String message = resolve(exc.getErrorCode().getMessageKey(), exc.getArgs());
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(exc.getErrorCode().name(), message));
    }

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ErrorResponse> handleOrderException(OrderException exc) {
        log.warn("Order business rule violation [code={}]", exc.getErrorCode());
        String message = resolve(exc.getErrorCode().getMessageKey(), exc.getArgs());
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(exc.getErrorCode().name(), message));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(PaymentException exc) {
        log.warn("Payment business rule violation [code={}]", exc.getErrorCode());
        String message = resolve(exc.getErrorCode().getMessageKey(), exc.getArgs());
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(exc.getErrorCode().name(), message));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException exc) {
        log.warn("Resource not found [code={}]", exc.getErrorCode());
        String message = resolve(exc.getErrorCode().getMessageKey());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exc.getErrorCode().name(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception exc) {
        log.error("Unhandled exception", exc);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private String resolve(String key) {
        return messageSource.getMessage(key, null, Locale.getDefault());
    }

    private String resolve(String key, Object[] args) {
        return messageSource.getMessage(key, args, Locale.getDefault());
    }
}
