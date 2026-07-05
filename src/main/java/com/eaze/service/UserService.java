package com.eaze.service;

import com.eaze.model.User;
import com.eaze.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public User register(User user){
        User newUser = new User();
        newUser.setFullName(user.getFullName());
        newUser.setPassword(user.getPassword());
        newUser.setEmail(user.getEmail());

        userRepository.save(newUser);
        return newUser;
    }



}
