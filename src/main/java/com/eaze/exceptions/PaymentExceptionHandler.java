package com.eaze.exceptions;

import com.eaze.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse> handlePaymentException(PaymentException ex) {
        ApiResponse res = new ApiResponse();
        res.setMessage(ex.getMessage());
        return new ResponseEntity<>(res, ex.getStatus());
    }
}
