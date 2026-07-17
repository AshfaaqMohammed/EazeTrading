package com.eaze.controller;

import com.eaze.model.User;
import com.eaze.request.UserLoginRequest;
import com.eaze.response.AuthResponse;
import com.eaze.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(
            @RequestBody
            User user
    ) throws Exception {

        AuthResponse response = authService.register(user);
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody
            UserLoginRequest user
    ) throws Exception {

        AuthResponse response = authService.login(user);
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @PostMapping("/two-factor/otp/{otp}")
    public ResponseEntity<AuthResponse> verifyLoginOtp(
            @PathVariable String otp,
            @RequestParam String id) throws Exception {

        AuthResponse response = authService.verifyLoginOtp(otp, id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
