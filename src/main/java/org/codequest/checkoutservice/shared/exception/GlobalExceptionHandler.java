package org.codequest.checkoutservice.shared.exception;

import org.codequest.checkoutservice.cart.exception.CartException;
import org.codequest.checkoutservice.order.exception.OrderException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(CartException.class)
    public ResponseEntity<ErrorResponse> handleCartException(CartException exc) {
        String message = resolve(exc.getErrorCode().getMessageKey());
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(exc.getErrorCode().name(), message));
    }

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ErrorResponse> handleOrderException(OrderException exc) {
        String message = resolve(exc.getErrorCode().getMessageKey());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exc.getErrorCode().name(), message));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException exc) {
        String message = resolve(exc.getErrorCode().getMessageKey());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exc.getErrorCode().name(), message));
    }

    private String resolve(String key) {
        return messageSource.getMessage(key, null, Locale.getDefault());
    }
}
