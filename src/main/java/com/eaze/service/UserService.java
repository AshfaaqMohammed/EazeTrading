package com.eaze.service;

import com.eaze.config.JwtProvider;
import com.eaze.model.User;
import com.eaze.repository.UserRepository;
import com.eaze.response.AuthResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public AuthResponse register(User user) throws Exception {

        User isEmailExist = userRepository.findByEmail(user.getEmail());
        if (isEmailExist != null){
            throw new Exception("Email is already used with another account.");
        }

        User newUser = new User();
        newUser.setFullName(user.getFullName());
        newUser.setPassword(user.getPassword());
        newUser.setEmail(user.getEmail());

        userRepository.save(newUser);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                user.getPassword()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        String jwt = JwtProvider.generateToken(auth);

        AuthResponse response = new AuthResponse();
        response.setJwt(jwt);
        response.setStatus(true);
        response.setMessage("register success");

        return response;
    }



}
