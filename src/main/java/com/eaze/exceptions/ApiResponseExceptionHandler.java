package com.eaze.exceptions;

import com.eaze.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    // Failed authentication (wrong password, unknown user) is a client error.
    // Return 401 Unauthorized with a JSON body instead of Spring's default 403,
    // so the frontend can handle invalid credentials cleanly.
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiResponse> handleAuthenticationException(Exception ex) {
        ApiResponse res = new ApiResponse();
        res.setMessage("Invalid email or password");
        return new ResponseEntity<>(res, HttpStatus.UNAUTHORIZED);
    }
}
