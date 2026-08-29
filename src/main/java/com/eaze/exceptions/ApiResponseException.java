package com.eaze.exceptions;

import org.springframework.http.HttpStatus;

public class ApiResponseException extends RuntimeException{
    private final HttpStatus status;

    public ApiResponseException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
