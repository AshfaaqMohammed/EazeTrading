package com.eaze.controller;

import com.eaze.model.User;
import com.eaze.model.dto.UserLoginRequest;
import com.eaze.response.AuthResponse;
import com.eaze.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(
            @RequestBody
            User user
    ) throws Exception {

        AuthResponse response = userService.register(user);
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody
            UserLoginRequest user
    ) throws Exception {

        AuthResponse response = userService.login(user);
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

}
