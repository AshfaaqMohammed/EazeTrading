package com.eaze.exceptions;

import com.eaze.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiResponseExceptionHandler {
    @ExceptionHandler(ApiResponseException.class)
    public ResponseEntity<ApiResponse> handleApiResponseException(ApiResponseException ex) {
        ApiResponse res = new ApiResponse();
        res.setMessage(ex.getMessage());
        return new ResponseEntity<>(res, ex.getStatus());
    }
}
