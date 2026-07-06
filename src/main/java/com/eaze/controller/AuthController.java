package com.eaze.controller;

import com.eaze.model.User;
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

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(
            @RequestBody
            User user
    ) throws Exception {

        AuthResponse response = userService.register(user);
        return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

}
